package com.xuxiaojian.aipassagecreator.api.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatisticsDTO implements Serializable {

    private static final long serialVersionUID = 2026061008L;

    private Long totalPaymentCount;

    private Long succeededPaymentCount;

    private Long refundedPaymentCount;
}
