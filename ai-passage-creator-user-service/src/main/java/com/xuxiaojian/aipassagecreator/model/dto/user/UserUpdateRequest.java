package com.xuxiaojian.aipassagecreator.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserUpdateRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 21:50
 * @Version 1.0
 */
@Data
public class UserUpdateRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
