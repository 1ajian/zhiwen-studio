package com.xuxiaojian.aipassagecreator.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.xuxiaojian.aipassagecreator.model.entity.PaymentRecord;

import java.util.List;

/**
 * ClassName: PaymentService
 * Package: com.xuxiaojian.aipassagecreator.service
 * Description:
 *  支付服务
 * @Author 阿健
 * @Create 2026-06-02 18:47
 * @Version 1.0
 */
public interface PaymentService {

    /**
     * 创建 VIP 永久会员支付会话
     * @param userId
     * @return
     * @throws StripeException
     */
    String createVipPaymentSession(Long userId) throws StripeException;

    /**
     * 处理支付成功回调
     * @param session
     */
    void handlePaymentSuccess(Session session);

    /**
     * 处理退款
     * @param userId
     * @param reason
     * @throws StripeException
     */
    boolean handleRefund(Long userId,String reason) throws StripeException;


    /**
     * 验证 Webhook 签名
     * @param payload 请求体
     * @param sigHeader 签名头
     * @return
     * @throws Exception
     */
    Event constructEvent(String payload,String sigHeader) throws Exception;

    /**
     * 获取用户支付记录
     * @param userId
     * @return
     */
    List<PaymentRecord> getPaymentRecords(Long userId);
}
