package com.xuxiaojian.aipassagecreator.publisher_listen;

import com.xuxiaojian.aipassagecreator.config.RefundCompensationProperties;
import com.xuxiaojian.aipassagecreator.model.enums.RefundCompensationStatusEnum;
import com.xuxiaojian.aipassagecreator.publisher_listen.event.RefundCompensationEvent;
import com.xuxiaojian.aipassagecreator.publisher_listen.store.RefundCompensationEventStore;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 退款补偿事件发布器。
 * 先保存事件快照再发布消息，避免消费者收到消息时查不到上下文。
 */
@Component
public class RedisRefundCompensationPublisher implements RefundCompensationPublisher {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RefundCompensationEventStore refundCompensationEventStore;

    @Resource
    private RefundCompensationProperties refundCompensationProperties;

    /**
     * 发布补偿事件
     * @param event 补偿事件
     */
    @Override
    public void publish(RefundCompensationEvent event) {
        RefundCompensationEvent existingEvent = refundCompensationEventStore.getEvent(event.getEventId());
        if (existingEvent != null) {
            if (RefundCompensationStatusEnum.SUCCESS.getValue().equals(existingEvent.getStatus())) {
                return;
            }
            if (RefundCompensationStatusEnum.PROCESSING.getValue().equals(existingEvent.getStatus())) {
                return;
            }
            event = existingEvent;
        }
        if (event.getStatus() == null || RefundCompensationStatusEnum.FAILED.getValue().equals(event.getStatus())) {
            event.setStatus(RefundCompensationStatusEnum.PENDING.getValue());
        }
        //保存事件
        refundCompensationEventStore.saveEvent(event);
        //发送事件
        stringRedisTemplate.convertAndSend(refundCompensationProperties.getChannel(), event.getEventId());
    }
}
