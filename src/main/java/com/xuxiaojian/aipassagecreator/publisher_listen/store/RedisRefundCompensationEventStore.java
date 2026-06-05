package com.xuxiaojian.aipassagecreator.publisher_listen.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuxiaojian.aipassagecreator.config.RefundCompensationProperties;
import com.xuxiaojian.aipassagecreator.model.enums.RefundCompensationStatusEnum;
import com.xuxiaojian.aipassagecreator.publisher_listen.event.RefundCompensationEvent;
import jakarta.annotation.Resource;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的退款补偿事件存储实现。
 * 使用字符串序列化保存事件快照，并使用 Redisson 延迟队列管理待重试事件。
 */
@Component
public class RedisRefundCompensationEventStore implements RefundCompensationEventStore {

    private static final String EVENT_KEY_PREFIX = "payment:refund:compensate:event:";

    private static final String LOCK_KEY_PREFIX = "payment:refund:compensate:lock:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RefundCompensationProperties refundCompensationProperties;

    @Resource
    private RedissonClient redissonClient;

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
        deleteEvent(eventId);
    }

    @Override
    public void scheduleRetry(RefundCompensationEvent event) {
        saveEvent(event);
        if (event.getNextRetryTime() != null) {
            long delayMillis = Math.max(0L, Duration.between(LocalDateTime.now(), event.getNextRetryTime()).toMillis());
            RBlockingDeque<String> blockingDeque = redissonClient.getBlockingDeque(refundCompensationProperties.getRetryQueueName());
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
            delayedQueue.offer(event.getEventId(), delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 删除重试任务。
     * 当前已改为 Redisson 延迟队列，队列消息到期后会自动迁移到阻塞队列，此处无需显式删除。
     * @param eventId
     */
    @Override
    public void removeRetry(String eventId) {
        // Redisson 延迟队列场景下不再维护 ZSet，因此这里保留空实现以兼容旧接口。
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
     * 获取时间戳区间的全部事件Id。
     * 当前短期重试已改为 Redisson 延迟队列，不再需要业务侧定时扫描。
     * @param now
     * @param limit
     * @return
     */
    @Override
    public List<String> pollRetryEventIds(LocalDateTime now, int limit) {
        return Collections.emptyList();
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
