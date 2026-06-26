package com.ddarahakit.commerce.common.model;

import lombok.Getter;

/**
 * 에러 코드 관리 (commerce-service 에서 사용하는 코드만 추출)
 */
@Getter
public enum BaseResponseStatus {
    /**
     * 20000 : 요청 성공
     */
    SUCCESS(true, 20000, "요청에 성공하였습니다."),

    /**
     * 30000 : Request 오류, Validation 오류
     */
    REQUEST_ERROR(false, 30001, "입력값을 확인해주세요."),

    /**
     * 40000 : Response 오류 (주문/장바구니/코스)
     */
    ORDERS_VALIDATION_FAIL(false, 40003, "결제 정보가 잘못되었습니다."),
    ORDERS_NOT_ORDERED(false, 40005, "결제 정보가 없습니다. 구매 후 이용해주세요."),
    ORDERS_NOT_PAID(false, 40040, "결제 완료된 주문이 아닙니다."),
    ORDERS_ALREADY_REFUNDED(false, 40041, "이미 환불된 주문입니다."),
    ORDERS_REFUND_FAIL(false, 40042, "환불 처리 중 오류가 발생했습니다."),
    ORDERS_ALREADY_PAID(false, 40043, "이미 결제 완료된 주문입니다."),

    CART_ALREADY_EXISTS(false, 40006, "이미 장바구니에 추가된 코스입니다."),
    CART_NOT_FOUND(false, 40007, "장바구니에 해당 항목이 존재하지 않습니다."),
    CART_ALREADY_PURCHASED(false, 40008, "이미 구매한 강의입니다."),

    COURSE_NOT_FOUND(false, 40060, "코스를 찾을 수 없습니다."),

    /**
     * 50000 : Database 오류
     */
    DATABASE_ERROR(false, 50001, "데이터베이스 연결에 실패하였습니다."),

    /**
     * 60000 : Server 오류
     */
    SERVER_ERROR(false, 60001, "서버와의 연결에 실패하였습니다.");


    private final boolean isSuccess;
    private final int code;
    private final String message;

    BaseResponseStatus(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }
}
