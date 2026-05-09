package com.example.demo.common.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class ProductChangeStatusDTO {
    @NotNull(message = "商品ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;
}