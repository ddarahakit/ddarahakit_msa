package com.ddarahakit.mentoring.common.model;

import lombok.Getter;

/**
 * 에러 코드 관리 (mentoring-service 에서 사용하는 코드만 추출)
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
    EXPIRED_JWT(false, 20001, "JWT 토큰이 만료되었습니다."),
    INVALID_JWT(false, 20002, "유효하지 않은 JWT입니다."),
    INVALID_USER_ROLE(false, 20003, "권한이 없는 유저의 접근입니다."),

    /**
     * 40000 : Response 오류
     */
    MENTORING_NOT_FOUND(false, 40020, "멘토링 세션을 찾을 수 없습니다."),
    MENTORING_FORBIDDEN(false, 40021, "멘토링 세션에 접근할 권한이 없습니다."),
    MENTORING_INVALID_MENTOR(false, 40022, "멘토 권한이 아닌 사용자입니다."),
    MENTORING_SESSION_CLOSED(false, 40023, "종료된 멘토링 세션입니다."),
    USER_NOT_FOUND(false, 40063, "사용자를 찾을 수 없습니다."),

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
