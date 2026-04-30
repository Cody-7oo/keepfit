package com.example.demo.common.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 查询个人优惠券入参
 */
@Data
public class CouponQueryDTO {

    @NotNull(message = "用户id不能为空")
    private Long userId;
}