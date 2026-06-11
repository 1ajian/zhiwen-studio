package com.xuxiaojian.aipassagecreator.aop;

import cn.hutool.core.util.StrUtil;
import com.xuxiaojian.aipassagecreator.annotation.AuthCheck;
import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.security.SessionUser;
import com.xuxiaojian.aipassagecreator.security.SessionUserHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 基于共享 SessionUser 的鉴权切面。
 * 这里不再注入 UserService，避免各业务服务为了鉴权直连用户库。
 */
@Slf4j
@Aspect
@Component
public class AuthInterceptor {

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        if (StrUtil.isBlank(mustRole)) {
            return joinPoint.proceed();
        }

        SessionUser loginUser = SessionUserHolder.getRequiredLoginUser();
        if (UserConstant.ADMIN_ROLE.equals(mustRole)
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }
}
