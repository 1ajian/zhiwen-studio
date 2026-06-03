package com.xuxiaojian.aipassagecreator.publisher_listen.process;

import com.xuxiaojian.aipassagecreator.model.entity.RefundCompensationTask;
import com.xuxiaojian.aipassagecreator.publisher_listen.event.RefundCompensationEvent;

/**
 * 退款补偿编排服务。
 * 用于统一处理 Redis 消费、短期重试、失败入库和数据库补偿任务执行。
 */
public interface RefundCompensationService {

    /**
     * 处理事件
     * @param eventId
     */
    void handleEvent(String eventId);

    void handleDbTask(RefundCompensationTask task);

    /**
     * 构建已经退款的事件
     * @param userId 用户id
     * @param paymentRecordId 支付记录id
     * @param stripePaymentIntentId 意向Id
     * @param reason 原因
     * @return
     */
    RefundCompensationEvent buildAlreadyRefundedEvent(Long userId, Long paymentRecordId,
                                                      String stripePaymentIntentId, String reason);
}
