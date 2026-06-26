package com.ddarahakit.commerce.domain.orders;

import com.ddarahakit.commerce.client.CourseClient;
import com.ddarahakit.commerce.common.model.BaseResponse;
import com.ddarahakit.commerce.common.model.BaseResponseStatus;
import com.ddarahakit.commerce.config.security.AuthUserDetails;
import com.ddarahakit.commerce.domain.orders.model.OrdersDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/orders")
@RestController
@Tag(name = "주문 컨트롤러")
public class OrdersController {

    private final OrdersService ordersService;
    private final CourseClient courseClient;

    @Operation(
            summary = "주문 확인",
            description = "코스 번호로 주문 확인.")
    @GetMapping("/check/{courseIdx}")
    public ResponseEntity<BaseResponse<CourseClient.CourseInfo>> check(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long courseIdx) {
        boolean result = ordersService.check(authUserDetails, courseIdx);

        if (result) {
            // 구매 확인됨 → 코스 정보(모놀리스)를 함께 반환
            CourseClient.CourseInfo response = courseClient.get(courseIdx).results();
            return ResponseEntity.ok(BaseResponse.success(response));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(BaseResponse.error(BaseResponseStatus.ORDERS_NOT_ORDERED));
        }
    }

    @Operation(
            summary = "주문 생성",
            description = "주문 금액과 코스 번호로 주문 생성.")
    @PostMapping("/create")
    public ResponseEntity<BaseResponse<OrdersDto.OrdersRes>> create(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @Valid @RequestBody OrdersDto.OrdersReq dto) {
        OrdersDto.OrdersRes response = ordersService.create(authUserDetails, dto);

        return ResponseEntity.ok(BaseResponse.success(response));
    }


    @Operation(
            summary = "결제 검증",
            description = "아임 포트 paymentId를 이용해서 실제 결제된 금액과 DB의 상품의 금액을 비교해서 검증.")
    @PostMapping("/verify")
    public ResponseEntity<BaseResponse<OrdersDto.VerifyRes>> verify(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @Valid @RequestBody OrdersDto.VerifyReq dto) {
        OrdersDto.VerifyRes response = ordersService.verify(authUserDetails, dto);

        return ResponseEntity.ok(BaseResponse.success(response));
    }


    @Operation(
            summary = "무료 주문 완료",
            description = "코스 salePrice 합계가 0원인 주문을 포트원 결제 검증 없이 서버에서 바로 결제완료 처리하고 수강 등록한다. 금액 0 여부는 서버에서 코스 가격으로 재검증한다.")
    @PostMapping("/{ordersIdx}/free-complete")
    public ResponseEntity<BaseResponse<OrdersDto.VerifyRes>> freeComplete(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long ordersIdx) {
        OrdersDto.VerifyRes response = ordersService.freeComplete(authUserDetails, ordersIdx);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "주문 환불",
            description = "결제 완료된 주문을 Portone API를 통해 전액 환불 처리.")
    @PostMapping("/{ordersIdx}/refund")
    public ResponseEntity<BaseResponse<OrdersDto.OrdersRes>> refund(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long ordersIdx,
            @Valid @RequestBody OrdersDto.RefundReq dto) {
        OrdersDto.OrdersRes response = ordersService.refund(authUserDetails, ordersIdx, dto);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "영수증 조회",
            description = "결제 완료된 본인 주문의 영수증 데이터를 조회한다.")
    @GetMapping("/{ordersIdx}/receipt")
    public ResponseEntity<BaseResponse<OrdersDto.ReceiptRes>> receipt(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long ordersIdx) {
        OrdersDto.ReceiptRes response = ordersService.receipt(authUserDetails, ordersIdx);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "주문 취소",
            description = "주문 IDX로 해당 주문 삭제")
    @DeleteMapping("/{ordersIdx}")
    public ResponseEntity<BaseResponse<OrdersDto.OrdersRes>> cancel(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long ordersIdx) {
        OrdersDto.OrdersRes response = ordersService.cancel(authUserDetails, ordersIdx);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
