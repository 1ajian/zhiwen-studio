package com.xuxiaojian.aipassagecreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 退款补偿配置。
 * 这里集中维护 Channel、重试间隔和调度参数，便于后续线上调优而不改业务代码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "payment.refund.compensation")
public class RefundCompensationProperties {

    //渠道名称
    private String channel = "payment:refund:compensate";

    /**
     * Redisson 延迟重试队列名称。
     * 这里只承载事件 ID，到期后交给消费者重新进入统一补偿处理链路。
     */
    private String retryQueueName = "payment:refund:compensate:retry:queue";

    //最大尝试次数
    private Integer maxRedisRetry;

    private List<Long> redisRetryDelays;

    /**
     * 历史 Redis ZSet 扫描周期配置。
     * 当前改为 Redisson 延迟队列后不再使用，保留字段仅为兼容已有配置文件。
     */
    private Long redisSchedulerDelay;

    /**
     * 处理超时分钟数
     */
    private Long processingTimeoutMinutes;

    private Long eventTtlDays;


}
