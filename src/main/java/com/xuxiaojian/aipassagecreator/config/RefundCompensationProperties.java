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

    //最大尝试次数
    private Integer maxRedisRetry;

    private List<Long> redisRetryDelays;

    private Long redisSchedulerDelay;

    /**
     * 处理超时分钟数
     */
    private Long processingTimeoutMinutes;

    private Long eventTtlDays;


}
