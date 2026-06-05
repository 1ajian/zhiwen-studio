package com.xuxiaojian.aipassagecreator.publisher_listen;

import com.xuxiaojian.aipassagecreator.config.RefundCompensationProperties;
import com.xuxiaojian.aipassagecreator.publisher_listen.process.RefundCompensationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 退款补偿延迟队列消费者。
 * 这里常驻阻塞读取到期事件 ID，并复用既有补偿服务处理，避免重新实现一套状态机。
 */
@Component
@Slf4j
public class RefundCompensationDelayQueueConsumer {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RefundCompensationProperties refundCompensationProperties;

    @Resource
    private RefundCompensationService refundCompensationService;

    @Resource
    @Qualifier("refundCompensationDelayQueueExecutor")
    private Executor refundCompensationDelayQueueExecutor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 启动后台消费线程。
     * 应用启动后立即监听到期重试事件，避免继续依赖定时扫描。
     */
    @PostConstruct
    public void start() {
        running.set(true);
        refundCompensationDelayQueueExecutor.execute(this::consumeLoop);
    }

    /**
     * 优雅停止消费循环。
     * 关闭时先打断循环标记，避免继续从队列中拉取新消息。
     */
    @PreDestroy
    public void stop() {
        running.set(false);
    }

    /**
     * 单次消费一个到期事件。
     * 该方法同时作为单元测试入口，便于验证取到 eventId 后会委派统一补偿服务。
     *
     * @throws InterruptedException 阻塞读取被中断时抛出
     */
    public void consumeNextEvent() throws InterruptedException {
        String eventId = getBlockingDeque().take();
        refundCompensationService.handleEvent(eventId);
    }

    private void consumeLoop() {
        while (running.get()) {
            try {
                String eventId = getBlockingDeque().poll(1, TimeUnit.SECONDS);
                if (eventId == null) {
                    continue;
                }
                refundCompensationService.handleEvent(eventId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("退款补偿延迟队列消费者已中断，准备停止消费循环");
                break;
            } catch (Exception e) {
                log.error("退款补偿延迟队列消费失败", e);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.info("退款补偿延迟队列消费者休眠时被中断，准备停止消费循环");
                    break;
                }
            }
        }
    }

    private RBlockingDeque<String> getBlockingDeque() {
        return redissonClient.getBlockingDeque(refundCompensationProperties.getRetryQueueName());
    }
}
