package com.ddarahakit.course.common.exception;

import com.ddarahakit.course.common.model.BaseResponse;
import com.ddarahakit.course.common.model.BaseResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import static com.ddarahakit.course.common.model.BaseResponseStatus.REQUEST_ERROR;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("[VALIDATION] 입력값 검증 실패: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BaseResponse.error(REQUEST_ERROR, errors));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse> handleBaseException(BaseException ex) {
        BaseResponseStatus status = ex.getStatus();

        return ResponseEntity
                .status(mapToHttpStatus(status.getCode()))
                .body(BaseResponse.error(status));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleException(Exception ex) {
        log.error("[SERVER] 예상치 못한 오류 발생: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(REQUEST_ERROR));
    }

    private int mapToHttpStatus(int statusCode) {
        // 40000번대 응답 오류 -> 400, 30000번대 요청 오류 -> 400, 기타 -> 500
        if (statusCode >= 40000 && statusCode < 50000) {
            return 400;
        } else if (statusCode >= 20000 && statusCode < 40000) {
            return 400;
        } else if (statusCode >= 50000 && statusCode < 60000) {
            return 500;
        } else {
            return 500;
        }
    }

}
