package com.example.demo.common.enums;

import lombok.Getter;

/**
 * 统一返回码枚举（大厂标准）
 */
@Getter
public enum ResultCodeEnum {

    // ====================== 通用 ======================
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(500, "系统异常"),
    PARAM_ERROR(400, "参数错误"),
    NOT_LOGIN(401, "未登录"),
    NO_PERMISSION(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),

    // ====================== 接口安全 / 限流 / 重放 / 签名 ======================
    REQUEST_TOO_FAST(429, "请求过于频繁，请稍后再试"),
    REPEAT_SUBMIT(8001, "请勿重复提交"),
    IDEMPOTENT_EXPIRE(8002, "请求已处理，请勿重复操作"),
    SIGN_ERROR(8003, "接口签名错误"),
    REQUEST_EXPIRED(8004, "请求已过期"),
    REPLAY_ATTACK(8005, "请勿重复请求"),

    // ====================== 商品 / 库存 ======================
    PRODUCT_NOT_EXIST(4001, "商品不存在"),
    STOCK_NOT_ENOUGH(4006, "商品库存不足"),
    STOCK_LOCK_FAIL(4007, "库存锁定失败"),

    // ====================== 购物车 ======================
    CART_NOT_EXIST(4002, "购物车记录不存在"),
    CART_EMPTY(5101, "购物车为空，无法下单"),

    // ====================== 订单 ======================
    ORDER_NOT_EXIST(5001, "订单不存在"),
    ORDER_NO_PERMISSION(5002, "无权限操作该订单"),
    ORDER_STATUS_NOT_ALLOW(5003, "订单状态不允许操作"),

    // ====================== 优惠券 ======================
    COUPON_TEMPLATE_NOT_EXIST(5201, "咖啡券模板未配置"),
    COUPON_NOT_EXIST(5202, "优惠券不存在"),
    COUPON_NO_PERMISSION(5203, "无权限使用该优惠券"),
    COUPON_USED_OR_EXPIRE(5204, "优惠券已使用或已过期"),

    // ====================== 用户 ======================
    USER_NOT_EXIST(6001, "用户不存在"),
    USER_PASSWORD_ERROR(6002, "密码错误"),
    USER_PHONE_EXIST(6003, "该手机号已注册"),
    LOGIN_FAIL(6004, "登录失败"),

    // ====================== 商家 ======================
    MERCHANT_NOT_EXIST(7001, "商家不存在"),
    MERCHANT_PASSWORD_ERROR(7002, "商家密码错误"),
    MERCHANT_PHONE_EXIST(7003, "商家手机号已注册");

    private final Integer code;
    private final String msg;

    ResultCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}