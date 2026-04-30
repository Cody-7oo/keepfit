package com.example.demo.common.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatusEnum {
    UN_PAY(0, "待支付"),
    PREPARE(1, "备餐中"),
    DELIVERING(2, "配送中"),
    COMPLETED(3, "已完成"),
    CANCEL(5, "已取消");

    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举desc
     */
    public static String getDescByCode(Integer code) {
        if (code == null) {
            return "";
        }
        for (OrderStatusEnum enums : OrderStatusEnum.values()) {
            if (enums.getCode().equals(code)) {
                // 修正：调用getter方法 getDesc()
                return enums.getDesc();
            }
        }
        return "";
    }
}