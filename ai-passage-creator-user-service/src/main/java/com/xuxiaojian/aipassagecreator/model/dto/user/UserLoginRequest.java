package com.xuxiaojian.aipassagecreator.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserLoginRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 20:27
 * @Version 1.0
 */
@Data
public class UserLoginRequest implements Serializable {
    private String userAccount;
    private String userPassword;
}

