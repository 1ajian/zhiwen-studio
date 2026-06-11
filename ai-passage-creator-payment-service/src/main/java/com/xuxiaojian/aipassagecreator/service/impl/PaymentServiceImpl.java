package com.xuxiaojian.aipassagecreator.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.xuxiaojian.aipassagecreator.api.user.UserRemoteClient;
import com.xuxiaojian.aipassagecreator.api.user.dto.UserPaymentProfileDTO;
import com.xuxiaojian.aipassagecreator.api.user.dto.VipUpdateRequest;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.RefundListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.xuxiaojian.aipassagecreator.config.StripeConfig;
import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.mapper.PaymentRecordMapper;
import com.xuxiaojian.aipassagecreator.model.entity.PaymentRecord;
import com.xuxiaojian.aipassagecreator.model.enums.PaymentStatusEnum;
import com.xuxiaojian.aipassagecreator.model.enums.ProductTypeEnum;
import com.xuxiaojian.aipassagecreator.service.PaymentService;
import com.xuxiaojian.aipassagecreator.publisher_listen.RefundCompensationPublisher;
import com.xuxiaojian.aipassagecreator.publisher_listen.process.RefundCompensationService;
import com.xuxiaojian.aipassagecreator.publisher_listen.execute.RefundCompensationExecutor;
import com.xuxiaojian.aipassagecreator.publisher_listen.event.RefundCompensationEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ClassName: PaymentServiceImpl
 * Package: com.xuxiaojian.aipassagecreator.service.impl
 * Description:
 *  支付服务实现
 * @Author 阿健
 * @Create 2026-06-02 18:56
 * @Version 1.0
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String CURRENCY_USD = "usd";

    private static final long CENTS_MULTIPLIER = 100L;

    @Resource
    private StripeConfig stripeConfig;

    @Resource
    private UserRemoteClient userRemoteClient;

    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    @Resource
    private RefundCompensationPublisher refundCompensationPublisher;

    @Resource
    private RefundCompensationService refundCompensationService;

    @Resource
    private RefundCompensationExecutor refundCompensationExecutor;

    /**
     * 创建 VIP 支付会话
     * @param userId
     * @return
     * @throws StripeException
     */
    @Override
    public String createVipPaymentSession(Long userId) throws StripeException {
        UserPaymentProfileDTO user = this.getUserById(userId);
        validateUserNotVip(user);
        ProductTypeEnum productType = ProductTypeEnum.VIP_PERMANENT;
        Session session = createStripeSession(userId,productType);
        savePaymentRecord(userId,session,productType);

        log.info("创建支付会话成功,userId={},sessionId={}",userId,session.getId());
        return session.getUrl();
    }

    /**
     * 处理支付成功回调
     * @param session
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(Session session) {
        String sessionId = session.getId();
        String userId = session.getMetadata().get("userId");
        String paymentIntentId = session.getPaymentIntent();
        PaymentRecord paymentRecord = findPaymentRecordBySessionId(sessionId);
        if (paymentRecord == null) {
            log.warn("支付记录不存在,sessionId={}",sessionId);
            return;
        }

        // 幂等性检查
        if (PaymentStatusEnum.SUCCEEDED.getValue().equals(paymentRecord.getStatus())) {
            log.info("支付记录已处理,sessionId={}",sessionId);
            return;
        }

        updatePaymentStatus(paymentRecord.getId(),PaymentStatusEnum.SUCCEEDED,paymentIntentId);
        upgradeUserToVip(Long.valueOf(userId));

        log.info("支付成功,用户已升级为VIP,userId={},sessionId={}",userId,sessionId);
    }

    /**
     * 处理退款回调
     * @param userId
     * @param reason
     * @throws StripeException
     */
    @Override
    //@Transactional(rollbackFor = Exception.class)
    public boolean handleRefund(Long userId, String reason) throws StripeException {
        UserPaymentProfileDTO user = getUserById(userId);
        validateUserIsVip(user);

        PaymentRecord paymentRecord = findLatesSuccessfulPayment(userId);
        if (paymentRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"未找到支付记录");
        }
        
        if (paymentRecord.getStripePaymentIntentId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"支付记录无效");
        }

        String stripePaymentIntentId = paymentRecord.getStripePaymentIntentId();
        //当前支付记录是否已经退款
        Refund alreadyRefund = findSucceededRefundByPaymentIntent(stripePaymentIntentId);
        if (alreadyRefund != null) {
            // 已经退款时转为异步补偿，避免接口线程同步执行本地修复逻辑
            RefundCompensationEvent event = refundCompensationService.buildAlreadyRefundedEvent(
                    userId, paymentRecord.getId(), stripePaymentIntentId, reason);
            refundCompensationPublisher.publish(event);
            log.info("检测到Stripe已退款,已发布退款补偿事件,eventId={},userId={},paymentRecordId={}",
                    event.getEventId(), userId, paymentRecord.getId());
            return true;
        }

        Refund refund = createStripeRefund(stripePaymentIntentId);
        if (!"succeeded".equals(refund.getStatus())) {
            return false;
        }

        refundCompensationExecutor.execute(userId, paymentRecord.getId(), reason);

        log.info("退款成功,已取消 VIP 身份,userId = {},refundId = {}",userId,refund.getId());
        return true;
    }

    /**
     * 查询是否已经退款
     * @param stripePaymentIntentId
     * @return
     * @throws StripeException
     */
    private Refund findSucceededRefundByPaymentIntent(String stripePaymentIntentId) throws StripeException {
        RefundListParams params = RefundListParams.builder()
                .setPaymentIntent(stripePaymentIntentId)
                .setLimit(10L)
                .build();

        for (Refund refund : Refund.list(params).getData()) {
            if ("succeeded".equals(refund.getStatus())) {
                return refund;
            }
        }
        return null;
    }



    /**
     * 校验当前用户是VIP
     * @param user
     */
    private void validateUserIsVip(UserPaymentProfileDTO user) {
        String userRole = user.getUserRole();
        LocalDateTime vipTime = user.getVipTime();
        if (!userRole.equals(UserConstant.VIP_ROLE) && vipTime == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"当前是非VIP用户");
        }
    }

    /**
     * 支付成功后异步通知的请求体会用密钥进行签名，防止伪造
     * @param payload 请求体
     * @param sigHeader 签名头
     * @return
     * @throws Exception
     */
    @Override
    public Event constructEvent(String payload, String sigHeader) throws Exception {
        return Webhook.constructEvent(payload,sigHeader,stripeConfig.getWebhookSecret());
    }

    @Override
    public List<PaymentRecord> getPaymentRecords(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create().eq("userId", userId).orderBy("createTime",false);
        return paymentRecordMapper.selectListByQuery(queryWrapper);
    }

    private UserPaymentProfileDTO getUserById(Long userId) {
        BaseResponse<UserPaymentProfileDTO> response = userRemoteClient.getUserPaymentProfile(userId);
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"用户不存在");
        }
        return response.getData();
    }

    /**
     * 校验是否为 VIP 用户
     * @param user
     */
    private void validateUserNotVip(UserPaymentProfileDTO user) {
        String userRole = user.getUserRole();
        LocalDateTime vipTime = user.getVipTime();
        if (UserConstant.VIP_ROLE.equals(userRole) || vipTime != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"当前已是VIP用户");
        }
    }

    /**
     * 创建StripeSession会话
     * @param userId
     * @param productType
     * @return
     * @throws StripeException
     */
    private Session createStripeSession(Long userId, ProductTypeEnum productType) throws StripeException {
        long amountInCents = productType.getPrice().multiply(new BigDecimal(CENTS_MULTIPLIER)).longValue();
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(stripeConfig.getSuccessUrl())
                        .setCancelUrl(stripeConfig.getCancelUrl())
                        .addLineItem(buildLineItem(productType,amountInCents))
                        .putMetadata("userId",String.valueOf(userId))
                        .putMetadata("productType",productType.getValue())
                        .build();

        return Session.create(params);
    }

    /**
     * 构建项目
     * @param productType
     * @param amountInCents
     * @return
     */
    private SessionCreateParams.LineItem buildLineItem(ProductTypeEnum productType, long amountInCents) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(CURRENCY_USD)
                        .setUnitAmount(amountInCents)
                        .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(productType.getDescription())
                                        .setDescription("解锁全部高级功能，无限创作额度，终身有效")
                                        .build()
                        ).build())
                .build();
    }

    /**
     * 保存支付记录信息
     * @param userId
     * @param session
     * @param productType
     */
    private void savePaymentRecord(Long userId, Session session, ProductTypeEnum productType) {
        PaymentRecord record = PaymentRecord.builder()
                .amount(productType.getPrice())
                .currency(CURRENCY_USD)
                .userId(userId)
                .stripeSessionId(session.getId())
                .status(PaymentStatusEnum.PENDING.getValue())
                .productType(productType.getValue())
                .description(productType.getDescription())
                .build();
        paymentRecordMapper.insert(record);
    }

    /**
     * 升级用户为 VIP
     * @param userId
     */
    private void upgradeUserToVip(Long userId) {
        VipUpdateRequest request = new VipUpdateRequest();
        request.setUserId(userId);
        request.setReason("支付成功开通 VIP");
        BaseResponse<Boolean> response = userRemoteClient.activateVip(request);
        if (response == null || response.getCode() != 0 || !Boolean.TRUE.equals(response.getData())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"远程开通 VIP 失败");
        }
    }

    /**
     * 更新支付状态
     * @param id
     * @param paymentStatus
     * @param paymentIntentId
     */
    private void updatePaymentStatus(Long id, PaymentStatusEnum paymentStatus, String paymentIntentId) {
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setId(id);
        paymentRecord.setStatus(paymentStatus.getValue());
        paymentRecord.setStripePaymentIntentId(paymentIntentId);
        paymentRecordMapper.update(paymentRecord);
    }

    /**
     * 通过sessionId获取支付记录
     * @param sessionId
     * @return
     */
    private PaymentRecord findPaymentRecordBySessionId(String sessionId) {
        PaymentRecord paymentRecord = paymentRecordMapper.selectOneByQuery
                (QueryWrapper.create().eq("stripeSessionId", sessionId));
        return paymentRecord;
    }

    /**
     * 创建Stripe退款
     * @param stripePaymentIntentId
     * @return
     * @throws StripeException
     */
    private Refund createStripeRefund(String stripePaymentIntentId) throws StripeException {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(stripePaymentIntentId)
                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .build();
        return Refund.create(params);
    }

    /**
     * 找到最新支付成功的支付记录
     * @param userId
     * @return
     */
    private PaymentRecord findLatesSuccessfulPayment(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create().eq("userId", userId)
                .eq("status", PaymentStatusEnum.SUCCEEDED.getValue())
                .orderBy("createTime", false);
        PaymentRecord paymentRecord = paymentRecordMapper.selectOneByQuery(queryWrapper);
        return paymentRecord;
    }

}
