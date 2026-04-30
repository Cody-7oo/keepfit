package com.example.demo.common.enums;

import lombok.Getter;

@Getter
public enum TemplateStatusEnum {
    OFF(0, "下架"),
    ON(1, "上架");

    private final Integer code;
    private final String desc;

    TemplateStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}