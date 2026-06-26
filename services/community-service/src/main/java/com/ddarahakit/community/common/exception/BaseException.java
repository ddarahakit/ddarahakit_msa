package com.ddarahakit.community.common.exception;

import com.ddarahakit.community.common.model.BaseResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BaseException extends RuntimeException {
    private BaseResponseStatus status;

    public BaseException(BaseResponseStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static BaseException of(BaseResponseStatus status) {
        return new BaseException(status, status.getMessage());
    }
}
