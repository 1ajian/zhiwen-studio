package com.xuxiaojian.aipassagecreator.schedule;

import com.mybatisflex.core.query.QueryWrapper;
import com.xuxiaojian.aipassagecreator.config.RefundCompensationProperties;
import com.xuxiaojian.aipassagecreator.mapper.RefundCompensationTaskMapper;
import com.xuxiaojian.aipassagecreator.model.entity.RefundCompensationTask;
import com.xuxiaojian.aipassagecreator.model.enums.RefundCompensationStatusEnum;
import com.xuxiaojian.aipassagecreator.publisher_listen.store.RefundCompensationEventStore;
import com.xuxiaojian.aipassagecreator.publisher_listen.RefundCompensationPublisher;
import com.xuxiaojian.aipassagecreator.publisher_listen.process.RefundCompensationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款补偿调度器。
 * 分别负责 Redis 短期重试与数据库失败任务重试，同时兜底回收超时卡住的 PROCESSING 任务。
 */
@Component
@Slf4j
public class RefundCompensationScheduler {

    @Resource
    private RefundCompensationProperties refundCompensationProperties;

    @Resource
    private RefundCompensationEventStore refundCompensationEventStore;

    @Resource
    private RefundCompensationPublisher refundCompensationPublisher;

    @Resource
    private RefundCompensationTaskMapper refundCompensationTaskMapper;

    @Resource
    private RefundCompensationService refundCompensationService;

    @Scheduled(fixedDelayString = "${payment.refund.compensation.redis-scheduler-delay}")
    public void retryRedisEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<String> eventIds = refundCompensationEventStore.pollRetryEventIds(now, 20);
        for (String eventId : eventIds) {
            var event = refundCompensationEventStore.getEvent(eventId);
            if (event == null || RefundCompensationStatusEnum.SUCCESS.getValue().equals(event.getStatus())) {
                refundCompensationEventStore.removeRetry(eventId);
                continue;
            }
            event.setStatus(RefundCompensationStatusEnum.PENDING.getValue());
            event.setProcessingStartTime(null);
            refundCompensationPublisher.publish(event);
            refundCompensationEventStore.removeRetry(eventId);
        }
    }

    @Scheduled(cron = "0 30 1 * * *")
    @Async("refundCompensationTaskExecutor")
    public void retryDbTasks() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .in("status", RefundCompensationStatusEnum.PENDING.getValue(),RefundCompensationStatusEnum.PROCESSING.getValue(),RefundCompensationStatusEnum.FAILED.getValue())
                .orderBy("createTime", true);
        List<RefundCompensationTask> tasks = refundCompensationTaskMapper.selectListByQuery(queryWrapper);
        for (RefundCompensationTask task : tasks) {
            refundCompensationService.handleDbTask(task);
        }
    }

}
