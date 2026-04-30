package com.example.demo.common.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class CartUpdateDTO {

    @NotNull(message = "购物车id不能为空")
    private Long id;

    @NotNull(message = "数量不能为空")
    private Integer num;
}