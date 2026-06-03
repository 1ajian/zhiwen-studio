package com.xuxiaojian.aipassagecreator.publisher_listen.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuxiaojian.aipassagecreator.config.RefundCompensationProperties;
import com.xuxiaojian.aipassagecreator.model.enums.RefundCompensationStatusEnum;
import com.xuxiaojian.aipassagecreator.publisher_listen.event.RefundCompensationEvent;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的退款补偿事件存储实现。
 * 使用字符串序列化保存事件快照，并使用 ZSet 管理待重试事件。
 */
@Component
public class RedisRefundCompensationEventStore implements RefundCompensationEventStore {

    private static final String EVENT_KEY_PREFIX = "payment:refund:compensate:event:";

    private static final String RETRY_KEY = "payment:refund:compensate:retry";

    private static final String LOCK_KEY_PREFIX = "payment:refund:compensate:lock:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RefundCompensationProperties refundCompensationProperties;

    /**
     * 保存或者更改事件到Redis
     * @param event
     */
    @Override
    public void saveEvent(RefundCompensationEvent event) {
        stringRedisTemplate.opsForValue().set(buildEventKey(event.getEventId()), writeValue(event),
                refundCompensationProperties.getEventTtlDays(), TimeUnit.DAYS);
    }

    /**
     * 根据事件Id获取事件
     * @param eventId
     * @return
     */
    @Override
    public RefundCompensationEvent getEvent(String eventId) {
        String json = stringRedisTemplate.opsForValue().get(buildEventKey(eventId));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RefundCompensationEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析退款补偿事件失败", e);
        }
    }

    /**
     * 标记事件正在处理
     * @param eventId
     * @param processingStartTime
     */
    @Override
    public void markProcessing(String eventId, LocalDateTime processingStartTime) {
        RefundCompensationEvent event = getEvent(eventId);
        if (event == null) {
            return;
        }
        event.setStatus(RefundCompensationStatusEnum.PROCESSING.getValue());
        event.setProcessingStartTime(processingStartTime);
        saveEvent(event);
    }

    /**
     * 标记成功 保存事件 移除重试
     * @param eventId
     */
    @Override
    public void markSuccess(String eventId) {
        RefundCompensationEvent event = getEvent(eventId);
        if (event == null) {
            return;
        }
        removeRetry(eventId);
        deleteEvent(eventId);
    }

    @Override
    public void scheduleRetry(RefundCompensationEvent event) {
        saveEvent(event);
        if (event.getNextRetryTime() != null) {
            stringRedisTemplate.opsForZSet().add(RETRY_KEY, event.getEventId(), event.getNextRetryTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
    }

    /**
     * 删除重试任务
     * @param eventId
     */
    @Override
    public void removeRetry(String eventId) {
        stringRedisTemplate.opsForZSet().remove(RETRY_KEY, eventId);
    }

    /**
     * 删除事件详情。
     * 当失败事件转入数据库补偿后，Redis 无需继续长期保留详情快照。
     *
     * @param eventId 事件 ID
     */
    @Override
    public void deleteEvent(String eventId) {
        stringRedisTemplate.delete(buildEventKey(eventId));
        releaseProcessingLock(eventId);
    }

    /**
     * 获取时间戳区间的全部事件Id
     * @param now
     * @param limit
     * @return
     */
    @Override
    public List<String> pollRetryEventIds(LocalDateTime now, int limit) {
        long maxScore = now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        var values = stringRedisTemplate.opsForZSet().rangeByScore(RETRY_KEY, 0, maxScore, 0, limit);
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream().toList();
    }

    /**
     * 尝试获取锁
     * @param eventId
     * @return
     */
    @Override
    public boolean tryAcquireProcessingLock(String eventId) {
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(buildLockKey(eventId), "1",
                refundCompensationProperties.getProcessingTimeoutMinutes(), TimeUnit.MINUTES);
        return Boolean.TRUE.equals(locked);
    }

    /**
     * 释放锁
     * @param eventId
     */
    @Override
    public void releaseProcessingLock(String eventId) {
        stringRedisTemplate.delete(buildLockKey(eventId));
    }

    private String buildEventKey(String eventId) {
        return EVENT_KEY_PREFIX + eventId;
    }

    private String buildLockKey(String eventId) {
        return LOCK_KEY_PREFIX + eventId;
    }

    private String writeValue(RefundCompensationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化退款补偿事件失败", e);
        }
    }
}
