package com.ddarahakit.identity.common.model;

import lombok.Getter;

/**
 * 에러 코드 관리
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
    // Common
    REQUEST_ERROR(false, 30001, "입력값을 확인해주세요."),
    EXPIRED_JWT(false, 20001, "JWT 토큰이 만료되었습니다."),
    INVALID_JWT(false, 20002, "유효하지 않은 JWT입니다."),
    INVALID_USER_ROLE(false,20003,"권한이 없는 유저의 접근입니다."),
    INVALID_USER_INFO(false,20004,"이메일 또는 비밀번호를 확인해주세요."),
    INVALID_USER_DISABLED(false,20005,"이메일 인증이 필요합니다. 이메일을 확인해주세요."),
    DUPLICATE_USER_EMAIL(false,20006,"중복된 이메일입니다. 다른 이메일을 사용해주세요."),
    LECTURE_NOT_IN_COURSE(false,20007,"해당 강의는 해당 코스의 강의가 아닙니다."),
    ALREADY_LECTURE_COMPLETE(false,20008,"해당 강의를 이미 완료하였습니다."),
    INVALID_USER_PASSWORD(false,20009,"비밀번호가 일치하지 않습니다."),
    INVALID_USER_EMAIL(false,20010,"이메일 정보가 잘못되었습니다."),
    INVALID_EMAIL_RESET_TIMEOUT(false,20011,"이메일 변경 링크가 만료되었습니다. 다시 시도해주세요."),



    /**
     * 4000 : Response 오류
     */
    // Common
    RESPONSE_ERROR(false, 40001, "값을 불러오는데 실패하였습니다."),
    RESPONSE_NULL_ERROR(false,40002,"[NULL]입력된 IDX 값로 접근한 DB의 유효한 ROW가 존재하지 않습니다."),

    ORDERS_VALIDATION_FAIL(false, 40003, "결제 정보가 잘못되었습니다."),
    IAMPORT_ERROR(false, 40004, "결제 금액이 잘못되었습니다."),
    ORDERS_NOT_ORDERED(false, 40005, "결제 정보가 없습니다. 구매 후 이용해주세요."),
    ORDERS_NOT_PAID(false, 40040, "결제 완료된 주문이 아닙니다."),
    ORDERS_ALREADY_REFUNDED(false, 40041, "이미 환불된 주문입니다."),
    ORDERS_REFUND_FAIL(false, 40042, "환불 처리 중 오류가 발생했습니다."),
    ORDERS_ALREADY_PAID(false, 40043, "이미 결제 완료된 주문입니다."),

    CART_ALREADY_EXISTS(false, 40006, "이미 장바구니에 추가된 코스입니다."),
    CART_NOT_FOUND(false, 40007, "장바구니에 해당 항목이 존재하지 않습니다."),
    CART_ALREADY_PURCHASED(false, 40008, "이미 구매한 강의입니다."),

    POST_NOT_FOUND(false, 40010, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(false, 40011, "댓글을 찾을 수 없습니다."),
    SCRAP_ALREADY_EXISTS(false, 40012, "이미 스크랩한 게시글입니다."),
    SCRAP_NOT_FOUND(false, 40013, "스크랩 정보를 찾을 수 없습니다."),
    POST_UNAUTHORIZED(false, 40014, "게시글 작성자만 수정/삭제할 수 있습니다."),
    COMMENT_UNAUTHORIZED(false, 40015, "댓글 작성자만 수정/삭제할 수 있습니다."),
    ROADMAP_NOT_FOUND(false, 40030, "로드맵을 찾을 수 없습니다."),

    COURSE_NOT_FOUND(false, 40060, "코스를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(false, 40061, "카테고리를 찾을 수 없습니다."),
    LECTURE_NOT_FOUND(false, 40062, "강의를 찾을 수 없습니다."),
    USER_NOT_FOUND(false, 40063, "사용자를 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(false, 40064, "리뷰를 찾을 수 없습니다."),
    EMAIL_VERIFY_NOT_FOUND(false, 40065, "유효하지 않거나 만료된 인증 링크입니다."),

    FILE_EMPTY(false, 40050, "업로드할 파일이 없습니다."),
    FILE_INVALID_TYPE(false, 40051, "허용되지 않는 파일 형식입니다. 이미지 파일(png, jpg, jpeg, gif, webp)만 업로드할 수 있습니다."),
    FILE_INVALID_PATH(false, 40052, "잘못된 파일 경로입니다."),

    /**
     * 50000 : Database 오류
     */
    DATABASE_ERROR(false, 50001, "데이터베이스 연결에 실패하였습니다."),
    FILE_UPLOAD_FAIL(false, 50002, "파일 저장 중 오류가 발생했습니다."),

    /**
     * 60000 : Server 오류
     */
    SERVER_ERROR(false, 60001, "서버와의 연결에 실패하였습니다.");



    /**
     * 70000 : 커스텀
     */



    // 70000 : 필요시 만들어서 쓰세요


    private final boolean isSuccess;
    private final int code;
    private final String message;

    BaseResponseStatus(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;;
        this.code = code;
        this.message = message;
    }
}
