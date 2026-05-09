package com.example.demo.common.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class CartAddDTO {

    @NotNull(message = "商品id不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    private Integer num;

    // 后端赋值
    private Long userId;
}