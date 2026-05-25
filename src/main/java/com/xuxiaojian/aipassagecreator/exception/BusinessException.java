package com.xuxiaojian.aipassagecreator.exception;

import lombok.Getter;

/**
 * ClassName: BusinessException
 * Package: com.xuxiaojian.aipassagecreator.exception
 * Description:
 *  业务异常
 * @Author 阿健
 * @Create 2026-05-25 23:32
 * @Version 1.0
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code,String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode,String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
