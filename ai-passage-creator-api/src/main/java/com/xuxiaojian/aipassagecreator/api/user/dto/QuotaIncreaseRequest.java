package com.xuxiaojian.aipassagecreator.api.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 配额补偿请求。
 * 远程扣减成功但下游业务失败时，用该对象归还指定用户配额。
 */
@Data
public class QuotaIncreaseRequest implements Serializable {

    private static final long serialVersionUID = 2026061003L;

    private Long userId;

    private Integer amount;

    private String reason;
}
