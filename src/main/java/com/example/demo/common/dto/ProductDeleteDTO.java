package com.example.demo.common.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 商家删除商品 DTO
 * 企业标准传参格式
 */
@Data
public class ProductDeleteDTO {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long id;

}