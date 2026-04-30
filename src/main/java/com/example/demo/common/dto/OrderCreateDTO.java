package com.example.demo.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 订单创建请求DTO
 * 大厂：统一接收前端参数、参数校验
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "收货人不能为空")
    private String receiver;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "收货地址不能为空")
    private String address;

    // 可选：优惠券ID
    private Long userCouponId;
}