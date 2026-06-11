package com.xuxiaojian.aipassagecreator.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.xuxiaojian.aipassagecreator.model.enums.RefundCompensationStatusEnum;
import com.xuxiaojian.aipassagecreator.publisher_listen.event.RefundCompensationEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款补偿失败任务实体。
 * 当 Redis 短期重试耗尽后，使用该表持久化失败上下文，便于定时任务继续补偿。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "refund_compensation_task", camelToUnderline = false)
public class RefundCompensationTask implements Serializable {

    private static final long serialVersionUID = -4615207859723878214L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String eventId;

    private Long userId;

    private Long paymentRecordId;

    private String stripePaymentIntentId;

    private String reason;

    private String source;

    private String status;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private String lastError;

    private LocalDateTime processingStartTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 将 Redis 重试耗尽的事件转换为数据库补偿任务。
     * 这里把数据库任务初始化为待处理状态，确保定时任务能立即接管。
     *
     * @param event 失败事件
     * @param now 当前时间
     * @param lastError 最新错误信息
     * @return 持久化任务实体
     */
    public static RefundCompensationTask fromEvent(RefundCompensationEvent event, LocalDateTime now, String lastError) {
        return RefundCompensationTask.builder()
                .eventId(event.getEventId())
                .userId(event.getUserId())
                .paymentRecordId(event.getPaymentRecordId())
                .stripePaymentIntentId(event.getStripePaymentIntentId())
                .reason(event.getReason())
                .source(event.getSource())
                .status(RefundCompensationStatusEnum.PENDING.getValue())
                .retryCount(0)
                .nextRetryTime(now)
                .lastError(lastError)
                .processingStartTime(null)
                .createTime(now)
                .updateTime(now)
                .build();
    }
}
