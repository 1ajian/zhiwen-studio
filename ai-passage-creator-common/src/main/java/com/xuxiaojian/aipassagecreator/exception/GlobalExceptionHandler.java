package com.xuxiaojian.aipassagecreator.exception;

import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ClassName: GlobalExceptionHandler
 * Package: com.xuxiaojian.aipassagecreator.exception
 * Description:
 *  全局异常处理器
 * @Author 阿健
 * @Create 2026-05-25 23:32
 * @Version 1.0
 */
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e, HttpServletRequest request, HttpServletResponse response) {
        log.error("BusinessException",e);
        resetResponseForJsonIfNecessary(request, response);
        return ResultUtils.error(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e, HttpServletRequest request, HttpServletResponse response) {
        log.error("RuntimeException",e);
        resetResponseForJsonIfNecessary(request, response);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"系统错误");
    }

    /**
     * 文件下载场景在异常发生前可能已写入 Excel Content-Type。
     * 若响应尚未提交，则重置为 JSON，避免消息转换器按 Excel 类型写出 BaseResponse 失败。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     */
    private void resetResponseForJsonIfNecessary(HttpServletRequest request, HttpServletResponse response) {
        if (response == null || response.isCommitted()) {
            return;
        }
        String contentType = response.getContentType();
        if (contentType != null
                && contentType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            response.reset();
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(request != null && request.getCharacterEncoding() != null
                    ? request.getCharacterEncoding()
                    : "UTF-8");
        }
    }
}
