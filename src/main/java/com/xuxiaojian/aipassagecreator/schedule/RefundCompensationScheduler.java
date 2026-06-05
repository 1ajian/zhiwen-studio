package com.xuxiaojian.aipassagecreator.schedule;

import com.mybatisflex.core.query.QueryWrapper;
import com.xuxiaojian.aipassagecreator.mapper.RefundCompensationTaskMapper;
import com.xuxiaojian.aipassagecreator.model.entity.RefundCompensationTask;
import com.xuxiaojian.aipassagecreator.model.enums.RefundCompensationStatusEnum;
import com.xuxiaojian.aipassagecreator.publisher_listen.process.RefundCompensationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 退款补偿调度器。
 * 当前仅负责数据库失败任务重试。
 * Redis 短期重试已迁移到 Redisson 延迟队列，到期后由常驻消费者自动接管。
 */
@Component
@Slf4j
public class RefundCompensationScheduler {

    @Resource
    private RefundCompensationTaskMapper refundCompensationTaskMapper;

    @Resource
    private RefundCompensationService refundCompensationService;

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
