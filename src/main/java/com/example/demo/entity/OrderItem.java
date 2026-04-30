package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细实体（企业规范）
 * 订单快照：商品信息、价格、营养值永久保存
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer isDeleted;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称（快照）
     */
    private String productName;

    /**
     * 下单时单价（快照）
     */
    private BigDecimal price;

    /**
     * 购买数量
     */
    private Integer num;

    /**
     * 卡路里（单份）
     */
    private Double calorie;

    /**
     * 蛋白质（单份）
     */
    private Double protein;

    /**
     * 碳水化合物（单份）
     */
    private Double carbohydrate;

    /**
     * 脂肪/油脂（单份）
     */
    private Double fat;
}