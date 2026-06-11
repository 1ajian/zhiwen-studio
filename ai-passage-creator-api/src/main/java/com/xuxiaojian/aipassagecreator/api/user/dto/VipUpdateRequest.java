package com.xuxiaojian.aipassagecreator.api.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * VIP 状态变更请求。
 * 支付服务只提交支付上下文，用户服务负责本地事务内更新用户状态。
 */
@Data
public class VipUpdateRequest implements Serializable {

    private static final long serialVersionUID = 2026061004L;

    private Long userId;

    private Long paymentRecordId;

    private String reason;
}
