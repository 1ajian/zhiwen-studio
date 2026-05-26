package com.xuxiaojian.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserAddRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 21:48
 * @Version 1.0
 */
@Data
public class UserAddRequest implements Serializable {
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
