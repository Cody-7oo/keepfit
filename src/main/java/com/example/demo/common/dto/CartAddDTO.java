package com.example.demo.common.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class CartAddDTO {

    @NotNull(message = "用户id不能为空")
    private Long userId;

    @NotNull(message = "商品id不能为空")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    private Integer num;
}