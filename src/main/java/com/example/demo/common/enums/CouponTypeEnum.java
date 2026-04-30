package com.example.demo.common.enums;

import lombok.Getter;

@Getter
public enum CouponTypeEnum {
    COFFEE_COUPON(1,"咖啡券"),
    FULL_REDUCE(2,"满减券");

    private final Integer code;
    private final String desc;

    // 必须同时接收 code 和 desc 两个参数
    CouponTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}