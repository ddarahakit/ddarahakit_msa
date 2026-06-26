package com.ddarahakit.review.common.model;

import lombok.Getter;

/**
 * 에러 코드 관리 (review-service 에서 사용하는 코드만 추출)
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
     * 40000 : Response 오류 (코스/리뷰)
     */
    COURSE_NOT_FOUND(false, 40060, "코스를 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(false, 40080, "리뷰를 찾을 수 없습니다."),

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
