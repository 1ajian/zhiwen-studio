package com.xuxiaojian.aipassagecreator.publisher_listen.execute;

/**
 * 退款补偿执行器。
 * 把真正的本地补偿动作抽离出来，方便 Redis 监听器和数据库定时任务复用同一套幂等逻辑。
 */
public interface RefundCompensationExecutor {

    /**
     * 执行退款补偿。
     *
     * @param userId 用户 ID
     * @param paymentRecordId 支付记录 ID
     * @param reason 退款原因
     */
    void execute(Long userId, Long paymentRecordId, String reason);
}
