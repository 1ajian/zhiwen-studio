package com.xuxiaojian.aipassagecreator.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.xuxiaojian.aipassagecreator.api.statistics.dto.PaymentStatisticsDTO;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.common.ResultUtils;
import com.xuxiaojian.aipassagecreator.mapper.PaymentRecordMapper;
import com.xuxiaojian.aipassagecreator.model.enums.PaymentStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付服务内部统计接口。
 * 统计服务通过该接口聚合支付指标，不直接访问支付库。
 */
@RestController
@RequestMapping("/internal/payment")
public class InternalPaymentController {

    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    @GetMapping("/statistics")
    public BaseResponse<PaymentStatisticsDTO> getPaymentStatistics() {
        PaymentStatisticsDTO statistics = PaymentStatisticsDTO.builder()
                .totalPaymentCount(paymentRecordMapper.selectCountByQuery(QueryWrapper.create()))
                .succeededPaymentCount(paymentRecordMapper.selectCountByQuery(
                        QueryWrapper.create().eq("status", PaymentStatusEnum.SUCCEEDED.getValue())))
                .refundedPaymentCount(paymentRecordMapper.selectCountByQuery(
                        QueryWrapper.create().eq("status", PaymentStatusEnum.REFUNDED.getValue())))
                .build();
        return ResultUtils.success(statistics);
    }
}
