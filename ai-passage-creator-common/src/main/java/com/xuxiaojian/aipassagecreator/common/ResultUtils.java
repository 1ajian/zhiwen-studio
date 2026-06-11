package com.xuxiaojian.aipassagecreator.common;

import com.xuxiaojian.aipassagecreator.exception.ErrorCode;

/**
 * ClassName: ResultUtils
 * Package: com.xuxiaojian.aipassagecreator.common
 * Description:
 *  响应工具类
 * @Author 阿健
 * @Create 2026-05-25 23:31
 * @Version 1.0
 */
public class ResultUtils {
    /**
     * 成功
     * @param data 数据
     * @return 响应
     * @param <T> 类型
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0,"ok",data);
    }

    /**
     * 失败
     * @param errorCode 错误码枚举
     * @return 响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * @param code 错误码
     * @param message 错误信息
     * @return 响应
     */
    public static BaseResponse<?> error(int code,String message) {
        return new BaseResponse<>(code,message,null);
    }

    /**
     * 失败
     * @param errorCode 错误码枚举对象
     * @param message 错误信息
     * @return 响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode,String message) {
        return new BaseResponse<>(errorCode.getCode(),message,null);
    }
}
