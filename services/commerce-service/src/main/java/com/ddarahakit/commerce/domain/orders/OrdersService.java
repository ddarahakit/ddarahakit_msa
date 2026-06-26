package com.ddarahakit.commerce.domain.orders;

import com.ddarahakit.commerce.client.CourseClient;
import com.ddarahakit.commerce.common.exception.BaseException;
import com.ddarahakit.commerce.config.security.AuthUserDetails;
import com.ddarahakit.commerce.domain.orders.model.Orders;
import com.ddarahakit.commerce.domain.orders.model.OrdersDto;
import com.ddarahakit.commerce.domain.orders.model.OrdersItem;
import com.ddarahakit.commerce.messaging.outbox.OutboxAppender;
import com.ddarahakit.common.event.EventType;
import com.ddarahakit.common.event.Topics;
import com.ddarahakit.common.event.payload.OrderEvents;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import io.portone.sdk.server.payment.CancelPaymentResponse;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.Payment;
import io.portone.sdk.server.payment.PaymentClient;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.ddarahakit.commerce.common.model.BaseResponseStatus.*;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class OrdersService {
    private final PaymentClient portone;
    private final OrdersRepository ordersRepository;
    private final OrdersItemRepository ordersItemRepository;
    private final CourseClient courseClient;
    private final OutboxAppender outboxAppender;

    @Transactional
    public OrdersDto.OrdersRes create(AuthUserDetails authUserDetails, OrdersDto.OrdersReq dto) {
        if (dto.getCourseIdxList() == null || dto.getCourseIdxList().isEmpty()) {
            throw BaseException.of(REQUEST_ERROR);
        }

        Long userIdx = authUserDetails.getIdx();

        List<Long> courseIdxList = dto.getCourseIdxList();
        if (courseIdxList.size() != courseIdxList.stream().distinct().count()) {
            throw BaseException.of(ORDERS_VALIDATION_FAIL);
        }

        // 각 코스를 모놀리스(CourseClient)로 조회해 가격을 검증한다.
        List<CourseClient.CourseInfo> courses = new ArrayList<>();
        for (Long courseIdx : courseIdxList) {
            CourseClient.CourseInfo course = fetchCourse(courseIdx);

            boolean alreadyPurchased = ordersItemRepository
                    .existsByOrdersUserIdxAndOrdersPaidTrueAndOrdersRefundedFalseAndCourseIdx(userIdx, courseIdx);
            if (alreadyPurchased) {
                throw BaseException.of(ORDERS_VALIDATION_FAIL);
            }
            courses.add(course);
        }

        int totalPrice = courses.stream().mapToInt(CourseClient.CourseInfo::salePrice).sum();
        if (!Objects.equals(dto.getPaymentPrice(), totalPrice)) {
            throw BaseException.of(ORDERS_VALIDATION_FAIL);
        }

        Orders orders = dto.toEntity(userIdx);
        Orders savedOrders = ordersRepository.save(orders);

        List<OrdersItem> items = courses.stream()
                .map(course -> OrdersItem.builder()
                        .orders(savedOrders)
                        .courseIdx(course.idx())
                        .courseName(course.name())
                        .unitPrice(course.salePrice())
                        .build())
                .toList();
        orders.getItems().addAll(items);
        ordersRepository.save(orders);

        return OrdersDto.OrdersRes.of(orders);
    }

    @Transactional
    public OrdersDto.VerifyRes verify(AuthUserDetails authUserDetails, OrdersDto.VerifyReq dto) {
        Long userIdx = authUserDetails.getIdx();
        CompletableFuture<Payment> completableFuture = portone.getPayment(dto.getPaymentId());
        Payment payment = completableFuture.join();

        if (payment instanceof PaidPayment paidPayment) {
            Map<String, Object> customData = new GsonBuilder()
                    .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                    .create().fromJson(paidPayment.getCustomData(), Map.class);

            Long ordersIdx = toLong(customData.get("ordersIdx"));
            // items 를 fetch join 으로 한 번에 로딩 (N+1 제거). 가격은 item 스냅샷(unitPrice).
            Orders orders = ordersRepository.findUnpaidWithItemsForVerify(ordersIdx, userIdx).orElseThrow(
                    () -> new BaseException(ORDERS_NOT_ORDERED)
            );
            int totalPrice = orders.getItems().stream()
                    .mapToInt(OrdersItem::getUnitPrice)
                    .sum();

            if (paidPayment.getAmount().getTotal() != totalPrice) {
                throw BaseException.of(ORDERS_VALIDATION_FAIL);
            }

            // 멱등 확정: paid=false 인 경우에만 원자적으로 paid=true 로 전이.
            // 동시/중복 verify 시 단 1회만 성공(updated==1)하고 나머지는 ORDERS_ALREADY_PAID.
            int updated = ordersRepository.markPaidIfUnpaid(ordersIdx, dto.getPaymentId());
            if (updated == 0) {
                throw BaseException.of(ORDERS_ALREADY_PAID);
            }

            orders.setPaid(true);
            orders.setPaymentId(dto.getPaymentId());

            appendOrderPaid(orders, userIdx);

            return OrdersDto.VerifyRes.of(orders);
        } else {
            throw BaseException.of(ORDERS_VALIDATION_FAIL);
        }
    }

    /**
     * 무료(0원) 주문 완료 처리.
     * 포트원/결제 검증을 건너뛰고 서버에서 바로 결제완료 상태로 전이한다.
     * 보안: 클라이언트 값이 아닌 서버 스냅샷(item.unitPrice) 합계로 0원 여부를 재검증하여
     * 유료 강의를 무료로 우회 등록하는 것을 차단한다.
     */
    @Transactional
    public OrdersDto.VerifyRes freeComplete(AuthUserDetails authUserDetails, Long ordersIdx) {
        Long userIdx = authUserDetails.getIdx();
        Orders orders = ordersRepository
                .findUnpaidWithItemsForFreeComplete(ordersIdx, userIdx)
                .orElseThrow(() -> BaseException.of(ORDERS_NOT_ORDERED));

        // 서버 측 금액 재계산: 스냅샷 단가 합계가 0일 때만 무료 완료 허용.
        int totalPrice = orders.getItems().stream()
                .mapToInt(OrdersItem::getUnitPrice)
                .sum();
        if (totalPrice != 0) {
            throw BaseException.of(ORDERS_VALIDATION_FAIL);
        }

        // 무료 주문은 포트원 결제 ID 가 없으므로 충돌하지 않는 합성 식별자를 부여한다.
        String freePaymentId = "free_" + ordersIdx;

        int updated = ordersRepository.markPaidIfUnpaid(ordersIdx, freePaymentId);
        if (updated == 0) {
            throw BaseException.of(ORDERS_ALREADY_PAID);
        }

        orders.setPaid(true);
        orders.setPaymentId(freePaymentId);

        appendOrderPaid(orders, userIdx);

        return OrdersDto.VerifyRes.of(orders);
    }

    @Transactional
    public OrdersDto.OrdersRes cancel(AuthUserDetails authUserDetails, Long ordersIdx) {
        Orders orders = ordersRepository.findByIdxAndUserIdxAndPaidFalse(ordersIdx, authUserDetails.getIdx()).orElseThrow(
                () -> BaseException.of(ORDERS_NOT_ORDERED)
        );

        ordersRepository.delete(orders);

        return OrdersDto.OrdersRes.of(orders);
    }

    @Transactional
    public OrdersDto.OrdersRes refund(AuthUserDetails authUserDetails, Long ordersIdx, OrdersDto.RefundReq dto) {
        Long userIdx = authUserDetails.getIdx();
        // 1. 주문 조회 (본인 주문만)
        Orders orders = ordersRepository.findByIdxAndUserIdx(ordersIdx, userIdx)
                .orElseThrow(() -> BaseException.of(ORDERS_NOT_ORDERED));

        // 2. 결제 완료 여부 확인
        if (!Boolean.TRUE.equals(orders.getPaid())) {
            throw BaseException.of(ORDERS_NOT_PAID);
        }

        // 3. 이미 환불된 주문 확인
        if (Boolean.TRUE.equals(orders.getRefunded())) {
            throw BaseException.of(ORDERS_ALREADY_REFUNDED);
        }

        // 4. Portone 환불 API 호출 (전액 환불)
        try {
            CompletableFuture<CancelPaymentResponse> future = portone.cancelPayment(
                    orders.getPaymentId(), null, null, null,
                    dto.getReason(), null, null, null, null
            );
            future.join();
        } catch (Exception e) {
            log.error("[환불 실패] ordersIdx={}, paymentId={}, reason={}", ordersIdx, orders.getPaymentId(), e.getMessage());
            throw BaseException.of(ORDERS_REFUND_FAIL);
        }

        // 5. 환불 상태 업데이트
        orders.setRefunded(true);
        Orders saved = ordersRepository.save(orders);

        // 6. 아웃박스에 OrderRefunded 적재(같은 트랜잭션)
        List<Long> courseIdxList = saved.getItems().stream()
                .map(OrdersItem::getCourseIdx)
                .toList();
        String aggregateId = courseIdxList.isEmpty() ? String.valueOf(saved.getIdx())
                : String.valueOf(courseIdxList.get(0));
        outboxAppender.append(Topics.COMMERCE_ORDER, EventType.ORDER_REFUNDED, aggregateId,
                new OrderEvents.OrderRefunded(saved.getIdx(), userIdx, courseIdxList));

        return OrdersDto.OrdersRes.of(saved);
    }

    /**
     * 결제 내역 페이징 조회(본인 결제 완료 주문). items fetch join → 목록 N+1 없음.
     */
    public OrdersDto.PaymentPageRes payments(AuthUserDetails authUserDetails, org.springframework.data.domain.Pageable pageable) {
        return OrdersDto.PaymentPageRes.of(
                ordersRepository.findPaidPageByUserIdx(authUserDetails.getIdx(), pageable));
    }

    /**
     * 영수증 데이터 조회(본인 결제 완료 주문만).
     */
    public OrdersDto.ReceiptRes receipt(AuthUserDetails authUserDetails, Long ordersIdx) {
        Orders orders = ordersRepository.findReceipt(ordersIdx, authUserDetails.getIdx())
                .orElseThrow(() -> BaseException.of(ORDERS_NOT_ORDERED));
        return OrdersDto.ReceiptRes.of(orders);
    }

    public boolean check(AuthUserDetails authUserDetails, Long courseIdx) {
        boolean exists = ordersItemRepository
                .existsByOrdersUserIdxAndOrdersPaidTrueAndOrdersRefundedFalseAndCourseIdx(authUserDetails.getIdx(), courseIdx);
        if (!exists) {
            throw BaseException.of(ORDERS_NOT_ORDERED);
        }
        return true;
    }

    /** 결제 확정 시 OrderPaid 이벤트를 아웃박스에 적재(비즈니스 변경과 같은 트랜잭션). */
    private void appendOrderPaid(Orders orders, Long userIdx) {
        List<Long> courseIdxList = orders.getItems().stream()
                .map(OrdersItem::getCourseIdx)
                .toList();
        String aggregateId = courseIdxList.isEmpty() ? String.valueOf(orders.getIdx())
                : String.valueOf(courseIdxList.get(0));
        outboxAppender.append(Topics.COMMERCE_ORDER, EventType.ORDER_PAID, aggregateId,
                new OrderEvents.OrderPaid(orders.getIdx(), userIdx, courseIdxList, Instant.now()));
    }

    /** 모놀리스 가격 조회 + 코스 존재 검증. */
    private CourseClient.CourseInfo fetchCourse(Long courseIdx) {
        CourseClient.CourseResponse res = courseClient.get(courseIdx);
        if (res == null || !res.success() || res.results() == null) {
            throw BaseException.of(COURSE_NOT_FOUND);
        }
        return res.results();
    }

    private Long toLong(Object value) {
        if (value == null) {
            throw BaseException.of(ORDERS_VALIDATION_FAIL);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            throw BaseException.of(ORDERS_VALIDATION_FAIL);
        }
    }
}
