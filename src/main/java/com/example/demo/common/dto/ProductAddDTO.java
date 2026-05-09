package com.example.demo.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProductAddDTO {

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String name;

    /**
     * 商品图片
     */
    @NotBlank(message = "商品图片不能为空")
    private String image;

    /**
     * 商品价格
     */
    @NotNull(message = "商品价格不能为空")
    private BigDecimal price;

    /**
     * 库存
     */

    private Integer stock;

    /**
     * 分类ID
     */
    @NotNull(message = "分类id不能为空")
    private Integer category;

    /**
     * 重量(g)
     */
    private BigDecimal weight;

    /**
     * 卡路里(kcal)
     */
    @NotNull(message = "卡路里不能为空")
    private Double calorie;

    /**
     * 蛋白质含量(g)
     */
    @NotNull(message = "蛋白质不能为空")
    private Double protein;

    /**
     * 碳水化合物含量(g)
     */
    @NotNull(message = "碳水不能为空")
    private Double carbohydrate;

    /**
     * 脂肪含量(g)
     */
    @NotNull(message = "脂肪不能为空")
    private Double fat;

    /**
     * 商品描述
     */
    @NotBlank(message = "描述不能为空")
    private String description;

    /**
     * 所属商家ID（Controller里从token获取，不需要前端传）
     */
    private Long merchantId;
}