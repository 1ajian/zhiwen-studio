package com.xuxiaojian.aipassagecreator.publisher_listen.process;
import com.xuxiaojian.aipassagecreator.config.RefundCompensationProperties;
import com.xuxiaojian.aipassagecreator.mapper.RefundCompensationTaskMapper;
import com.xuxiaojian.aipassagecreator.model.entity.RefundCompensationTask;
import com.xuxiaojian.aipassagecreator.model.enums.RefundCompensationStatusEnum;
import com.xuxiaojian.aipassagecreator.publisher_listen.store.RefundCompensationEventStore;
import com.xuxiaojian.aipassagecreator.publisher_listen.event.RefundCompensationEvent;
import com.xuxiaojian.aipassagecreator.publisher_listen.execute.RefundCompensationExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 退款补偿编排服务实现。
 * 这里负责把不同载体的补偿流程收敛成统一的状态机，保证重试与入库逻辑一致。
 */
@Service
@Slf4j
public class RefundCompensationServiceImpl implements RefundCompensationService {

    private static final String ALREADY_REFUNDED_SOURCE = "ALREADY_REFUNDED";

    @Resource
    private RefundCompensationExecutor refundCompensationExecutor;

    @Resource
    private RefundCompensationEventStore refundCompensationEventStore;

    @Resource
    private RefundCompensationTaskMapper refundCompensationTaskMapper;

    @Resource
    private RefundCompensationProperties refundCompensationProperties;

    /**
     * 处理事件
     * @param eventId
     */
    @Override
    public void handleEvent(String eventId) {
        RefundCompensationEvent event = refundCompensationEventStore.getEvent(eventId);
        if (event == null) {
            log.warn("退款补偿事件不存在,eventId={}", eventId);
            return;
        }
        if (RefundCompensationStatusEnum.SUCCESS.getValue().equals(event.getStatus())) {
            return;
        }
        if (RefundCompensationStatusEnum.PROCESSING.getValue().equals(event.getStatus())
                && event.getProcessingStartTime() != null
                && event.getProcessingStartTime().isAfter(LocalDateTime.now()
                .minusMinutes(refundCompensationProperties.getProcessingTimeoutMinutes()))) {
            log.info("退款补偿事件仍在处理中,跳过重复消费,eventId={}", eventId);
            return;
        }
        // 分布式锁 setnx expire
        if (!refundCompensationEventStore.tryAcquireProcessingLock(eventId)) {
            log.info("退款补偿事件处理锁已被占用,跳过重复消费,eventId={}", eventId);
            return;
        }
        try {
            //标记处理
            refundCompensationEventStore.markProcessing(eventId, LocalDateTime.now());
            //执行处理
            refundCompensationExecutor.execute(event.getUserId(), event.getPaymentRecordId(), event.getReason());
            //标记成功 这里并没有直接删除事件对象（前面没有抛出异常，就代表是成功了）
            refundCompensationEventStore.markSuccess(eventId);
            log.info("退款补偿事件处理成功,eventId={}", eventId);
        } catch (Exception e) {
            //处理 受理后异常事件
            handleEventFailure(event, e);
        } finally {
            //释放处理过程锁
            refundCompensationEventStore.releaseProcessingLock(eventId);
        }
    }

    @Override
    public void handleDbTask(RefundCompensationTask task) {
        if (task == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = refundCompensationTaskMapper.markProcessing(task.getId(), now, now);
        if (updated <= 0) {
            return;
        }
        try {
            refundCompensationExecutor.execute(task.getUserId(), task.getPaymentRecordId(), task.getReason());
            refundCompensationTaskMapper.markSuccess(task.getId(), LocalDateTime.now());
        } catch (Exception e) {
            log.info("定时任务处理支付记录异常数据失败," + e.getMessage() + ",stripePaymentIntentId:" + task.getStripePaymentIntentId());
        }
    }

    /**
     * 构建已经退款事件
     * @param userId 用户id
     * @param paymentRecordId 支付记录id
     * @param stripePaymentIntentId 意向Id
     * @param reason 原因
     * @return
     */
    @Override
    public RefundCompensationEvent buildAlreadyRefundedEvent(Long userId, Long paymentRecordId,
                                                             String stripePaymentIntentId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        return RefundCompensationEvent.builder()
                .eventId(buildStableEventId(paymentRecordId, stripePaymentIntentId))
                .userId(userId)
                .paymentRecordId(paymentRecordId)
                .stripePaymentIntentId(stripePaymentIntentId)
                .reason(reason + ",补偿退款")
                .source(ALREADY_REFUNDED_SOURCE)
                .retryCount(0)
                .status(RefundCompensationStatusEnum.PENDING.getValue())
                .createdAt(now)
                .build();
    }

    /**
     *  处理失败事件 （进行重试）
     * @param event 事件
     * @param e
     */
    private void handleEventFailure(RefundCompensationEvent event, Exception e) {
        LocalDateTime failedAt = LocalDateTime.now();
        int nextRetryCount = event.getRetryCount() + 1;
        RefundCompensationRetryPolicy retryPolicy = new RefundCompensationRetryPolicy(
                refundCompensationProperties.getRedisRetryDelays());
        LocalDateTime nextRetryTime = retryPolicy.calculateNextRetryTime(failedAt, nextRetryCount);
        event.setRetryCount(nextRetryCount);
        event.setStatus(RefundCompensationStatusEnum.FAILED.getValue());
        event.setLastError(simplifyErrorMessage(e));
        event.setProcessingStartTime(null);
        event.setNextRetryTime(nextRetryTime);
        if (nextRetryTime == null || nextRetryCount > refundCompensationProperties.getMaxRedisRetry()) {
            persistFailedEvent(event);
            refundCompensationEventStore.removeRetry(event.getEventId());
            return;
        }

        refundCompensationEventStore.scheduleRetry(event);
        log.warn("退款补偿事件处理失败,准备重试,eventId={},retryCount={},nextRetryTime={}",
                event.getEventId(), nextRetryCount, nextRetryTime);
    }

    /**
     * 尝试超过最大次数进行持久化失败任务
     * @param event
     */
    private void persistFailedEvent(RefundCompensationEvent event) {
        RefundCompensationTask existingTask = refundCompensationTaskMapper.selectByEventId(event.getEventId());
        if (existingTask != null) {
            refundCompensationEventStore.deleteEvent(event.getEventId());
            return;
        }
        RefundCompensationTask task = RefundCompensationTask.fromEvent(event, LocalDateTime.now(), event.getLastError());
        refundCompensationTaskMapper.insert(task);
        refundCompensationEventStore.deleteEvent(event.getEventId());
        log.error("退款补偿事件重试耗尽,已转入数据库补偿,eventId={}", event.getEventId());
    }

    private String buildStableEventId(Long paymentRecordId, String stripePaymentIntentId) {
        String raw = paymentRecordId + ":" + stripePaymentIntentId + ":" + ALREADY_REFUNDED_SOURCE;
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String simplifyErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
