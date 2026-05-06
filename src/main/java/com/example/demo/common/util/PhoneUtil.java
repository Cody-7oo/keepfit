package com.example.demo.common.util;

/**
 * 手机号脱敏工具类
 */
public class PhoneUtil {

    /**
     * 手机号中间四位脱敏
     * 例：13433174289 → 134****4289
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}