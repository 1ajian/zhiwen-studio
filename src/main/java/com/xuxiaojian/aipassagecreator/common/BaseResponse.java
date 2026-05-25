package com.xuxiaojian.aipassagecreator.common;

import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: BaseResponse
 * Package: com.xuxiaojian.aipassagecreator.common
 * Description:
 *  通用响应类
 * @Author 阿健
 * @Create 2026-05-25 23:30
 * @Version 1.0
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public BaseResponse(int code,String message,T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(int code,T data) {
        this(code,"",data);
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }
}
