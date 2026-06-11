package com.xuxiaojian.aipassagecreator.publisher_listen.process;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款补偿重试时间策略。
 * Redis 短期补偿用到“超过次数返回空”，数据库持久化补偿可复用最后一个间隔持续退避。
 */
public class RefundCompensationRetryPolicy {

    /**
     * Redis重试延迟时间
     */
    private final List<Long> retryDelayMinutes;

    /**
     * 超过后再利用上一次延迟
     */
    private final boolean reuseLastDelayWhenExceeded;

    public RefundCompensationRetryPolicy(List<Long> retryDelayMinutes) {
        this(retryDelayMinutes, false);
    }

    public RefundCompensationRetryPolicy(List<Long> retryDelayMinutes, boolean reuseLastDelayWhenExceeded) {
        this.retryDelayMinutes = retryDelayMinutes;
        this.reuseLastDelayWhenExceeded = reuseLastDelayWhenExceeded;
    }

    /**
     * 计算下一次重试时间。
     * 时间基于“本次失败完成时刻”推算，避免任务尚未结束时被下一轮调度重复处理。
     *
     * @param failedAt 本次失败时间
     * @param retryCount 当前失败后的累计次数，1 表示第一次失败后的首次重试
     * @return 下一次重试时间，若返回 null 表示不再自动重试
     */
    public LocalDateTime calculateNextRetryTime(LocalDateTime failedAt, int retryCount) {
        if (failedAt == null || retryCount <= 0 || retryDelayMinutes == null || retryDelayMinutes.isEmpty()) {
            return null;
        }
        if (retryCount <= retryDelayMinutes.size()) {
            return failedAt.plusMinutes(retryDelayMinutes.get(retryCount - 1));
        }
        if (!reuseLastDelayWhenExceeded) {
            return null;
        }
        return failedAt.plusMinutes(retryDelayMinutes.get(retryDelayMinutes.size() - 1));
    }
}
