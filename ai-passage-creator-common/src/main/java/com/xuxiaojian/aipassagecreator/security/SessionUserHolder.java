package com.xuxiaojian.aipassagecreator.security;

import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前请求登录态读取工具。
 * 业务服务直接从共享 Session 读取用户摘要，避免跨服务依赖用户实体或用户库。
 */
public final class SessionUserHolder {

    private SessionUserHolder() {
    }

    public static SessionUser getCurrentLoginUser() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj instanceof SessionUser sessionUser) {
            return sessionUser;
        }
        return null;
    }

    public static SessionUser getRequiredLoginUser() {
        SessionUser sessionUser = getCurrentLoginUser();
        if (sessionUser == null || sessionUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return sessionUser;
    }
}
