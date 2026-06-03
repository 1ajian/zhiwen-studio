package com.xuxiaojian.aipassagecreator.publisher_listen.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款补偿事件。
 * 该事件既作为 Redis 发布订阅载荷，也作为 Redis 中事件状态快照的存储模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundCompensationEvent implements Serializable {

    private static final long serialVersionUID = -7999352351712211652L;

    private String eventId;

    private Long userId;

    private Long paymentRecordId;

    private String stripePaymentIntentId;

    private String reason;

    private String source;

    private Integer retryCount;

    /**
     * 事件处理状态 枚举类关联 RefundCompensationStatusEnum
     */
    private String status;

    private String lastError;

    /**
     * 下一次尝试时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 处理开始时间
     */
    private LocalDateTime processingStartTime;

    private LocalDateTime createdAt;
}
