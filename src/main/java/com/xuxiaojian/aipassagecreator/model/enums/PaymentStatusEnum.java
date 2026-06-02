package com.xuxiaojian.aipassagecreator.model.enums;

import lombok.Getter;

/**
 * ClassName: PaymentStatusEnum
 * Package: com.xuxiaojian.aipassagecreator.model.enums
 * Description:
 *  支付状态枚举
 * @Author 阿健
 * @Create 2026-06-02 18:13
 * @Version 1.0
 */
@Getter
public enum PaymentStatusEnum {

    PENDING("PENDING","待支付"),
    SUCCEEDED("SUCCEEDED","支付成功"),
    FAILED("FAILED","支付失败"),
    REFUNDED("REFUNDED","已退款")
    ;
    private final String value;

    private final String description;

    PaymentStatusEnum(String value,String description) {
        this.value = value;
        this.description = description;
    }

    public static PaymentStatusEnum getByValue(String value ) {
        if (value == null) {
            return null;
        }
        for (PaymentStatusEnum paymentStatusEnum : values()) {
            if (paymentStatusEnum.value.equals(value)) {
                return paymentStatusEnum;
            }

        }
        return null;
    }

}
