package com.xuxiaojian.aipassagecreator.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ClassName: LoginUserVO
 * Package: com.xuxiaojian.aipassagecreator.model.vo
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 20:28
 * @Version 1.0
 */
@Data
public class LoginUserVO implements Serializable {
    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /**
     * 成为会员时间
     */
    private LocalDateTime vipTime;

    private Integer quota;

}

