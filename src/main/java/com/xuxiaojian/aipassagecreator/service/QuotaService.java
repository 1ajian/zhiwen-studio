package com.xuxiaojian.aipassagecreator.service;

import com.xuxiaojian.aipassagecreator.model.entity.User;

/**
 * ClassName: QuotaService
 * Package: com.xuxiaojian.aipassagecreator.service
 * Description:
 *  配额服务接口
 * @Author 阿健
 * @Create 2026-06-02 22:27
 * @Version 1.0
 */
public interface QuotaService {

    /**
     * 检查用户是否有足够的额度
     * @param user
     * @return
     */
    boolean hasQuata(User user);

    /**
     * 消耗配额（扣减1次）
     * @param user
     * @return
     */
    void consumeQuota(User user);

    /**
     * 检查并消耗配额（原子操作）
     * 如果配额不足会抛出异常
     * @param user
     */
    void checkAndConsumeQuota(User user);
}
