package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健身餐商品实体（企业规范）
 * 核心：营养字段标准化、自动填充时间、注释完整、状态规范
 */
@Data
@TableName("product")
public class Product  implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableLogic
    private Integer isDeleted;
    /**
     * 商品主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品单价
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
     * 重量（g）
     */
    private Double weight;

    /**
     * 蛋白质（g）
     */
    private Double protein;

    /**
     * 碳水化合物（g）
     */
    private Double carbohydrate;

    /**
     * 脂肪/油脂（g）
     */
    private Double fat;

    /**
     * 卡路里（热量）
     */
    private Double calorie;

    private String description;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}