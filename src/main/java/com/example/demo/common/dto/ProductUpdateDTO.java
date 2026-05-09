package com.example.demo.common.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProductUpdateDTO {

    /**
     * 商品ID（必填）
     */
    @NotNull(message = "商品ID不能为空")
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 分类ID
     */
    private Integer category;

    /**
     * 重量(g)
     */
    private BigDecimal weight;

    /**
     * 卡路里(kcal)
     */
    private Double calorie;

    /**
     * 蛋白质含量(g)
     */
    private Double protein;

    /**
     * 碳水化合物含量(g)
     */
    private Double carbohydrate;

    /**
     * 脂肪含量(g)
     */
    private Double fat;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 所属商家ID（Controller里从token获取，不需要前端传）
     */
    private Long merchantId;
}