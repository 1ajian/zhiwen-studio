package com.xuxiaojian.aipassagecreator.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ClassName: PaymentRecord
 * Package: com.xuxiaojian.aipassagecreator.model.entity
 * Description:
 *  支付记录实体类
 * @Author 阿健
 * @Create 2026-06-02 18:23
 * @Version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "payment_record", camelToUnderline = false)
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = -3475521780086177611L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * Stripe Checkout Session ID
     */
    private String stripeSessionId;

    /**
     * Stripe 支付意向ID
     */
    private String stripePaymentIntentId;

    /**
     * 金额（美元）
     */
    private BigDecimal amount;

    /**
     * 货币
     */
    private String currency;

    /**
     * 状态：PENDING/SUCCEEDED/FAILED/REFUNDED
     */
    private String status;

    /**
     * 产品类型：VIP_PERMANENT
     */
    private String productType;

    /**
     * 描述
     */
    private String description;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
