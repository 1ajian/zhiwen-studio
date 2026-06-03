package com.xuxiaojian.aipassagecreator.publisher_listen.execute;

import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.mapper.PaymentRecordMapper;
import com.xuxiaojian.aipassagecreator.mapper.UserMapper;
import com.xuxiaojian.aipassagecreator.model.entity.PaymentRecord;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.model.enums.PaymentStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * 本地退款补偿执行器。
 * 将退款记录修复与 VIP 回退抽成独立 Bean，避免支付服务与异步补偿服务之间形成循环依赖。
 */
@Service
public class PaymentRefundCompensationExecutor implements RefundCompensationExecutor {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 执行本地退款补偿。
     * 这里统一复用事务边界，确保退款记录状态与用户 VIP 状态保持一致。
     *
     * @param userId 用户 ID
     * @param paymentRecordId 支付记录 ID
     * @param reason 退款原因
     */
    @Override
    public void execute(Long userId, Long paymentRecordId, String reason) {
        transactionTemplate.execute(status -> {
            updateRefundRecord(paymentRecordId, reason);
            revokeVipStatus(userId);
            return true;
        });
    }

    /**
     * 回退到非 VIP 角色。
     * 只有当前用户仍是 VIP 时才更新，避免重复补偿产生副作用。
     *
     * @param userId 用户 ID
     */
    private void revokeVipStatus(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            return;
        }
        String userRole = user.getUserRole();
        if (UserConstant.VIP_ROLE.equals(userRole)) {
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setVipTime(null);
            updateUser.setUserRole(UserConstant.DEFAULT_ROLE);
            userMapper.refundUpdateUser(updateUser);
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
