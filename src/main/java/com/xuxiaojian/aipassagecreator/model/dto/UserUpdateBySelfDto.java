package com.xuxiaojian.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserUpdateBySelfDto
 * Package: com.xuxiaojian.aipassagecreator.model.dto
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 22:29
 * @Version 1.0
 */
@Data
public class UserUpdateBySelfDto implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 当前密码
     */
    private String oldUserPassword;

    /**
     * 新密码
     */
    private String newUserPassword;

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


    private static final long serialVersionUID = 1L;
}
