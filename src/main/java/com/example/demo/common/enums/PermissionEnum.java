package com.example.demo.common.enums;

import lombok.Getter;

@Getter
public enum PermissionEnum {
    // 商家权限
    //@SaCheckLogin(type = "merchant")
    //    @SaCheckPermission("merchant:coupon:update")
    COUPON_ADD("merchant:coupon:add", "新增优惠券"),
    COUPON_UPDATE("merchant:coupon:update", "修改优惠券"),
    PRODUCT_MANAGE("merchant:product:manage", "商品管理"),
    ORDER_OP("merchant:order:op", "订单操作"),

    // 用户权限
    CART_OP("user:cart:op", "购物车操作"),
    ORDER_CREATE("user:order:create", "下单操作");

    private final String permission;
    private final String desc;

    PermissionEnum(String permission, String desc) {
        this.permission = permission;
        this.desc = desc;
    }
}