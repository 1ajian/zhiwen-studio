package com.xuxiaojian.aipassagecreator.security;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 共享登录态对象。
 * 微服务之间共享 Redis Session 时只保存该稳定模型，避免反序列化到某个服务私有实体失败。
 */
@Data
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 2026061001L;

    private Long id;

    private String userAccount;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;

    private LocalDateTime vipTime;

    private Integer quota;
}
