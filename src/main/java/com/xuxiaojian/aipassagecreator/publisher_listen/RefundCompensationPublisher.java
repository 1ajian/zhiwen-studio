package com.xuxiaojian.aipassagecreator.publisher_listen;

import com.xuxiaojian.aipassagecreator.publisher_listen.event.RefundCompensationEvent;

/**
 * 退款补偿事件发布器。
 * 统一封装事件投递与事件快照保存，避免 PaymentServiceImpl 直接依赖 Redis 细节。
 */
public interface RefundCompensationPublisher {

    /**
     * 发布退款补偿事件。
     *
     * @param event 补偿事件
     */
    void publish(RefundCompensationEvent event);
}
