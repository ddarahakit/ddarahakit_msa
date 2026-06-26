package com.ddarahakit.common.response;

/**
 * 전 서비스 공통 응답 포맷(모놀리스 규약 유지).
 * 게이트웨이/서비스가 동일 포맷으로 응답·에러를 반환한다.
 */
public record BaseResponse<T>(boolean success, int code, String message, T results) {

    private static final int SUCCESS_CODE = 20000;

    public static <T> BaseResponse<T> success(T results) {
        return new BaseResponse<>(true, SUCCESS_CODE, "요청에 성공하였습니다.", results);
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(false, code, message, null);
    }
}
