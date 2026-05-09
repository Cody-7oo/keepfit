package com.example.demo.common.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class CartDeleteDTO {
    // 商品ID（必传）
    @NotNull(message = "商品id不能为空")
    private Long productId;
}