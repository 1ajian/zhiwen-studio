package com.xuxiaojian.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserRegisterRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 20:25
 * @Version 1.0
 */
@Data
public class UserRegisterRequest implements Serializable {
    private String userAccount;
    private String userPassword;
    private String checkPassword;
}

