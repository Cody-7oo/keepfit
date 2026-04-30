package com.example.demo.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProductUpdateDTO {

    @NotNull(message = "商品id不能为空")
    private Long id;

    @NotBlank(message = "商品名称不能为空")
    private String productName;

    @NotBlank(message = "商品图片不能为空")
    private String productImg;

    @NotNull(message = "商品价格不能为空")
    private BigDecimal price;

    @NotNull(message = "分类id不能为空")
    private Long categoryId;

    @NotNull(message = "卡路里不能为空")
    private Double calorie;

    @NotNull(message = "蛋白质不能为空")
    private Double protein;

    @NotNull(message = "碳水不能为空")
    private Double carbohydrate;

    @NotNull(message = "脂肪不能为空")
    private Double fat;

    @NotNull(message = "商家id不能为空")
    private Long merchantId;

    @NotNull(message = "描述不能为空")
    private String description;
}