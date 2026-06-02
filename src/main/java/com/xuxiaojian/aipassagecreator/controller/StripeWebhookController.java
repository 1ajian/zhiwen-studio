package com.xuxiaojian.aipassagecreator.controller;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.service.PaymentService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * ClassName: StripeWebhookController
 * Package: com.xuxiaojian.aipassagecreator.controller
 * Description:
 *  Stripe Webhook 控制器
 * @Author 阿健
 * @Create 2026-06-02 21:55
 * @Version 1.0
 */
@RestController
@RequestMapping("/webhook")
@Slf4j
@Hidden
public class StripeWebhookController {
    @Resource
    private PaymentService paymentService;

    @PostMapping("/stripe")
    public String handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // 验证 Webhook 签名
            Event event = paymentService.constructEvent(payload, sigHeader);
            log.info("收到 Stripe Webhook 事件,type = {}",event.getType());

            //处理事件
            switch (event.getType()) {
                case "checkout.session.completed":
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR,"无法解析 Session 对象"));
                    paymentService.handlePaymentSuccess(session);
                    break;

                case "checkout.session.async_payment_succeeded":
                    Session asyncSession =(Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR,"无法解析 Session 对象"));
                    paymentService.handlePaymentSuccess(asyncSession);
                    break;

                default:
                    log.info("未处理的事件类型:{}",event.getType());
                    break;
            }

            return "success";
        } catch (Exception e) {
            log.error("处理 Stripe Webhook 失败", e);
            return "error";
        }
    }
}
