package com.example.demo.common.enums;

import lombok.Getter;

@Getter
public enum UserCouponStatusEnum {
    UN_USED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期");

    private final Integer code;
    private final String desc;

    UserCouponStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}