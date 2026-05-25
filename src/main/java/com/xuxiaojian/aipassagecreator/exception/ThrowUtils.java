package com.xuxiaojian.aipassagecreator.exception;

/**
 * ClassName: ThrowUtils
 * Package: com.xuxiaojian.aipassagecreator.exception
 * Description:
 *  异常工具类
 * @Author 阿健
 * @Create 2026-05-25 23:32
 * @Version 1.0
 */

public class ThrowUtils {

    /**
     * 条件成立则抛出异常
     * @param condition 条件
     * @param runtimeException 异常类
     */
    public static void throwIf(boolean condition,RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛出异常
     * @param condition 条件
     * @param errorCode 错误码枚举
     */
    public static void throwIf(boolean condition,ErrorCode errorCode) {
        throwIf(condition,new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛出异常
     * @param condition 条件
     * @param errorCode 错误码枚举对象
     * @param message 异常消息
     */
    public static void throwIf(boolean condition,ErrorCode errorCode,String message) {
        throwIf(condition,new BusinessException(errorCode,message));
    }
}
