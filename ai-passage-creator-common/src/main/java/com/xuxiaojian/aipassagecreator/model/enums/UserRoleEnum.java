package com.xuxiaojian.aipassagecreator.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * ClassName: UserRoleEnum
 * Package: com.xuxiaojian.aipassagecreator.model.enums
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 20:51
 * @Version 1.0
 */
@Getter
public enum UserRoleEnum {
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    private UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据枚举值获取枚举对象
     *
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }

        return null;
    }
}
