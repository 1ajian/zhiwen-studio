package com.xuxiaojian.aipassagecreator.model.enums;

import lombok.Getter;

/**
 * 退款补偿任务状态枚举。
 * 用于统一 Redis 事件状态与数据库补偿任务状态，避免多处硬编码字符串。
 */
@Getter
public enum RefundCompensationStatusEnum {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "处理成功"),
    FAILED("FAILED", "处理失败");

    private final String value;

    private final String description;

    RefundCompensationStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
