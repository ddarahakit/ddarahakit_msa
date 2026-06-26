package com.ddarahakit.community.common.model;

import lombok.Getter;

/**
 * 에러 코드 관리 (community-service 에서 사용하는 코드만 추출)
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
    POST_NOT_FOUND(false, 40010, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(false, 40011, "댓글을 찾을 수 없습니다."),
    SCRAP_ALREADY_EXISTS(false, 40012, "이미 스크랩한 게시글입니다."),
    SCRAP_NOT_FOUND(false, 40013, "스크랩 정보를 찾을 수 없습니다."),
    POST_UNAUTHORIZED(false, 40014, "게시글 작성자만 수정/삭제할 수 있습니다."),
    COMMENT_UNAUTHORIZED(false, 40015, "댓글 작성자만 수정/삭제할 수 있습니다."),
    USER_NOT_FOUND(false, 40063, "사용자를 찾을 수 없습니다."),

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


    private final boolean isSuccess;
    private final int code;
    private final String message;

    BaseResponseStatus(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }
}
