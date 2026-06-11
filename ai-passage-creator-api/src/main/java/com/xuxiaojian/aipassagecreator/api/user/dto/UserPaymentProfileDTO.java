package com.xuxiaojian.aipassagecreator.api.user.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付服务需要的最小用户资料。
 * 避免支付服务依赖用户实体或用户表结构。
 */
@Data
public class UserPaymentProfileDTO implements Serializable {

    private static final long serialVersionUID = 2026061005L;

    private Long id;

    private String userRole;

    private LocalDateTime vipTime;

    private Integer quota;
}
