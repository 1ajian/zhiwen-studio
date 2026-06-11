package com.xuxiaojian.aipassagecreator.api.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 配额扣减请求。
 * 文章服务只传递必要用户摘要，实际原子扣减在用户服务内完成。
 */
@Data
public class QuotaConsumeRequest implements Serializable {

    private static final long serialVersionUID = 2026061002L;

    private Long userId;

    private String userRole;
}
