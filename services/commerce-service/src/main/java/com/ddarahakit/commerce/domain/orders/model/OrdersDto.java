package com.ddarahakit.commerce.domain.orders.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public class OrdersDto {

    @Builder
    @Data
    public static class OrdersReq {
        @NotNull(message = "결제 금액은 필수 입력값입니다.")
        @Min(value = 0, message = "결제 금액은 0 이상이어야합니다.")
        @Schema(description = "결제 금액", required = true, example = "1000")
        private Integer paymentPrice;

        @NotEmpty(message = "코스 IDX List는 필수 입력값입니다.")
        @Schema(description = "코스 IDX List", required = true, example = "[1,2,3]")
        private List<Long> courseIdxList;

        public Orders toEntity(Long userIdx) {
            return Orders.builder()
                    .paid(false)
                    // refunded 를 명시적으로 false 로 초기화한다.
                    // 미설정 시 NULL 로 INSERT 되어 구매판정/수강목록 쿼리의
                    // 'refunded = false' 조건(NULL != false)에 걸려 주문이 제외된다.
                    .refunded(false)
                    .paymentPrice(paymentPrice)
                    .userIdx(userIdx)
                    .build();
        }
    }


    @Builder
    @Data
    public static class OrdersRes {
        private Long ordersIdx;
        private Boolean paid;
        private Boolean refunded;

        public static OrdersRes of(Orders entity) {
            return OrdersRes.builder()
                    .ordersIdx(entity.getIdx())
                    .paid(entity.getPaid())
                    .refunded(entity.getRefunded())
                    .build();
        }
    }

    @Builder
    @Data
    public static class RefundReq {
        @NotBlank(message = "환불 사유는 필수 입력값입니다.")
        @Schema(description = "환불 사유", required = true, example = "구매 실수")
        private String reason;
    }


    @Builder
    @Data
    public static class VerifyReq {
        @Pattern(message = "paymentId는 'imp_'로 시작하고 뒤에 랜덤 ID와 날짜 형식이어야 합니다.",
                regexp = "^imp_[a-f0-9]{16}_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}$")
        @Schema(description = "포트원 PaymentId", required = true, example = "imp_652790891096")
        private String paymentId;
    }

    @Builder
    @Data
    public static class VerifyRes {
        private String paymentId;
        private OrdersRes orders;

        public static VerifyRes of(Orders entity) {
            return VerifyRes.builder()
                    .paymentId(entity.getPaymentId())
                    .orders(OrdersRes.of(entity))
                    .build();
        }
    }

    // ── 결제 내역(목록) ─────────────────────────────
    @Builder
    @Data
    public static class PaymentItemRes {
        @Schema(description = "코스 IDX")
        private Long courseIdx;
        @Schema(description = "코스 이름")
        private String courseName;
        @Schema(description = "코스 결제가(할인가, 결제 시점 스냅샷)")
        private int salePrice;

        public static PaymentItemRes of(OrdersItem i) {
            return PaymentItemRes.builder()
                    .courseIdx(i.getCourseIdx())
                    .courseName(i.getCourseName())
                    .salePrice(i.getUnitPrice())
                    .build();
        }
    }

    @Builder
    @Data
    public static class PaymentRes {
        @Schema(description = "주문 IDX")
        private Long ordersIdx;
        @Schema(description = "포트원 결제 ID")
        private String paymentId;
        @Schema(description = "총 결제 금액")
        private int paymentPrice;
        @Schema(description = "환불 여부")
        private Boolean refunded;
        @Schema(description = "결제 일시(updatedAt 기준)")
        private java.util.Date paidAt;
        @Schema(description = "결제 코스 목록")
        private java.util.List<PaymentItemRes> items;

        public static PaymentRes of(Orders o) {
            return PaymentRes.builder()
                    .ordersIdx(o.getIdx())
                    .paymentId(o.getPaymentId())
                    .paymentPrice(o.getPaymentPrice())
                    .refunded(o.getRefunded())
                    .paidAt(o.getUpdatedAt())
                    .items(o.getItems().stream().map(PaymentItemRes::of).toList())
                    .build();
        }
    }

    @Builder
    @Data
    public static class PaymentPageRes {
        private java.util.List<PaymentRes> payments;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;

        public static PaymentPageRes of(org.springframework.data.domain.Page<Orders> page) {
            return PaymentPageRes.builder()
                    .payments(page.getContent().stream().map(PaymentRes::of).toList())
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .hasNext(page.hasNext())
                    .build();
        }
    }

    // ── 영수증 ─────────────────────────────
    @Builder
    @Data
    public static class ReceiptRes {
        @Schema(description = "주문 IDX")
        private Long ordersIdx;
        @Schema(description = "포트원 결제 ID")
        private String paymentId;
        @Schema(description = "총 결제 금액")
        private int paymentPrice;
        @Schema(description = "환불 여부")
        private Boolean refunded;
        @Schema(description = "결제 일시")
        private java.util.Date paidAt;
        @Schema(description = "구매 항목")
        private java.util.List<PaymentItemRes> items;

        public static ReceiptRes of(Orders o) {
            return ReceiptRes.builder()
                    .ordersIdx(o.getIdx())
                    .paymentId(o.getPaymentId())
                    .paymentPrice(o.getPaymentPrice())
                    .refunded(o.getRefunded())
                    .paidAt(o.getUpdatedAt())
                    .items(o.getItems().stream().map(PaymentItemRes::of).toList())
                    .build();
        }
    }

}
