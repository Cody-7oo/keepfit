package com.example.demo.common.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {
    USER("user", "普通用户"),
    MERCHANT("merchant", "商家");

    private final String code;
    private final String desc;

    RoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}