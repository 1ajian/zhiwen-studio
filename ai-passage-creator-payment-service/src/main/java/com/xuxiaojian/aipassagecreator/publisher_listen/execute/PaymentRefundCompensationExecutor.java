package com.xuxiaojian.aipassagecreator.publisher_listen.execute;

import com.xuxiaojian.aipassagecreator.api.user.UserRemoteClient;
import com.xuxiaojian.aipassagecreator.api.user.dto.VipUpdateRequest;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.mapper.PaymentRecordMapper;
import com.xuxiaojian.aipassagecreator.model.entity.PaymentRecord;
import com.xuxiaojian.aipassagecreator.model.enums.PaymentStatusEnum;
import jakarta.annotation.Resource;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 退款补偿执行器。
 * 将退款记录修复与 VIP 回退抽成独立 Bean，避免支付服务与异步补偿服务之间形成循环依赖。
 * 这里通过 Seata AT 模式托管支付库与用户库的分布式事务边界。
 */
@Service
public class PaymentRefundCompensationExecutor implements RefundCompensationExecutor {

    @Resource
    private UserRemoteClient userRemoteClient;

    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    /**
     * 执行退款补偿。
     * 这里以支付服务作为全局事务发起方，确保退款记录状态与用户 VIP 状态跨服务一致。
     *
     * @param userId 用户 ID
     * @param paymentRecordId 支付记录 ID
     * @param reason 退款原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @GlobalTransactional(name = "payment-refund-compensation", rollbackFor = Exception.class)
    public void execute(Long userId, Long paymentRecordId, String reason) {
        updateRefundRecord(paymentRecordId, reason);
        revokeVipStatus(userId);
    }

    /**
     * 回退到非 VIP 角色。
     * 只有当前用户仍是 VIP 时才更新，避免重复补偿产生副作用。
     *
     * @param userId 用户 ID
     */
    private void revokeVipStatus(Long userId) {
        VipUpdateRequest request = new VipUpdateRequest();
        request.setUserId(userId);
        request.setReason("退款补偿取消 VIP");
        BaseResponse<Boolean> response = userRemoteClient.cancelVip(request);
        if (response == null || response.getCode() != 0 || !Boolean.TRUE.equals(response.getData())) {
            throw new IllegalStateException("远程取消 VIP 失败");
        }
    }

    /**
     * 更新支付记录退款信息。
     * 只有支付记录仍为成功状态时才写退款信息，保证重复执行幂等。
     *
     * @param recordId 支付记录 ID
     * @param reason 退款原因
     */
    private void updateRefundRecord(Long recordId, String reason) {
        PaymentRecord paymentRecord = paymentRecordMapper.selectOneById(recordId);
        if (paymentRecord == null) {
            return;
        }
        if (PaymentStatusEnum.SUCCEEDED.getValue().equals(paymentRecord.getStatus())) {
            PaymentRecord updateRecord = new PaymentRecord();
            updateRecord.setId(recordId);
            updateRecord.setStatus(PaymentStatusEnum.REFUNDED.getValue());
            updateRecord.setRefundTime(LocalDateTime.now());
            updateRecord.setRefundReason(reason);
            paymentRecordMapper.update(updateRecord);
        }
    }
}
