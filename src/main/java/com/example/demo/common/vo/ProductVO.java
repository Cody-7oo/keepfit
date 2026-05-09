package com.example.demo.common.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVO {
    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 商品状态 0-下架 1-上架
     */
    private Integer status;

    /**
     * 分类ID
     */
    private Integer category;

    /**
     * 重量(g)
     */
    private BigDecimal weight;

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
     * 卡路里(kcal)
     */
    private Double calorie;

    /**
     * 商品描述
     */

    private String description;

    /**
     * 所属商家ID
     */
    private Long merchantId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}