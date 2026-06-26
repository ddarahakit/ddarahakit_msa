package com.ddarahakit.course.common.model;

import lombok.Getter;

/**
 * 에러 코드 관리 (course-service 에서 사용하는 코드만 추출)
 */
@Getter
public enum BaseResponseStatus {
    /**
     * 20000 : 요청 성공
     */
    SUCCESS(true, 20000, "요청에 성공하였습니다."),

    /**
     * 20000 : 코스/강의 도메인 규칙 오류
     */
    LECTURE_NOT_IN_COURSE(false, 20007, "해당 강의는 해당 코스의 강의가 아닙니다."),
    ALREADY_LECTURE_COMPLETE(false, 20008, "해당 강의를 이미 완료하였습니다."),

    /**
     * 30000 : Request 오류, Validation 오류
     */
    REQUEST_ERROR(false, 30001, "입력값을 확인해주세요."),

    /**
     * 40000 : Response 오류 (코스/카테고리/강의/로드맵/수강)
     */
    ORDERS_NOT_ORDERED(false, 40005, "결제 정보가 없습니다. 구매 후 이용해주세요."),
    ROADMAP_NOT_FOUND(false, 40030, "로드맵을 찾을 수 없습니다."),
    COURSE_NOT_FOUND(false, 40060, "코스를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(false, 40061, "카테고리를 찾을 수 없습니다."),
    LECTURE_NOT_FOUND(false, 40062, "강의를 찾을 수 없습니다."),

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
