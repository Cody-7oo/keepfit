package com.example.demo.common.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 优惠券使用入参
 */
@Data
public class CouponUseDTO {

    @NotNull(message = "用户优惠券id不能为空")
    private Long userCouponId;

    @NotNull(message = "用户id不能为空")
    private Long userId;

    @NotNull(message = "订单号不能为空")
    private String orderNo;
}