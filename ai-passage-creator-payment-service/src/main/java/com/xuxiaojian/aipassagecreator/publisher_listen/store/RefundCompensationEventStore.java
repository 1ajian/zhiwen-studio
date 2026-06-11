package com.xuxiaojian.aipassagecreator.publisher_listen.store;

import com.xuxiaojian.aipassagecreator.publisher_listen.event.RefundCompensationEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款补偿事件存储接口。
 * 这里屏蔽 Redis 的具体存储结构，便于监听器与调度器复用。
 */
public interface RefundCompensationEventStore {

    void saveEvent(RefundCompensationEvent event);

    RefundCompensationEvent getEvent(String eventId);

    void markProcessing(String eventId, LocalDateTime processingStartTime);

    void markSuccess(String eventId);

    void scheduleRetry(RefundCompensationEvent event);

    void removeRetry(String eventId);

    void deleteEvent(String eventId);

    List<String> pollRetryEventIds(LocalDateTime now, int limit);

    boolean tryAcquireProcessingLock(String eventId);

    void releaseProcessingLock(String eventId);
}
