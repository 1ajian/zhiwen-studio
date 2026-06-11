package com.xuxiaojian.aipassagecreator.api.payment;

import com.xuxiaojian.aipassagecreator.api.statistics.dto.PaymentStatisticsDTO;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ai-payment-service", path = "/api/internal/payment")
public interface PaymentRemoteClient {

    @GetMapping("/statistics")
    BaseResponse<PaymentStatisticsDTO> getPaymentStatistics();
}
