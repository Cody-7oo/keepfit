package com.example.demo.common.enums;

import lombok.Getter;

@Getter
public enum ProductStatusEnum {
    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "上架");

    private final Integer code;
    private final String desc;

    ProductStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}