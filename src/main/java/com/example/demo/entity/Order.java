package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.handler.SensitiveDataHandler;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表（企业级规范）
 * 统一字段、自动填充时间、状态规范、营养统计标准化
 */
@Data
@TableName("order_info")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer isDeleted;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单编号（全局唯一）
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 所属商家ID（权限核心字段）
     */
    private Long merchantId;

    // ====================== 收货信息 ======================
    /**
     * 收货人
     */
    @TableField(typeHandler = SensitiveDataHandler.class)
    private String receiver;

    /**
     * 收货电话
     */
    @TableField(typeHandler = SensitiveDataHandler.class)
    private String phone;

    /**
     * 收货地址
     */
    @TableField(typeHandler = SensitiveDataHandler.class)
    private String address;

    // ====================== 金额 & 营养统计 ======================
    /**
     * 订单总价
     */
    private BigDecimal totalPrice;

    /**
     * 总卡路里
     */
    private Double totalCalorie;

    /**
     * 总蛋白质
     */
    private Double totalProtein;

    /**
     * 总碳水化合物
     */
    private Double totalCarbohydrate;

    /**
     * 总脂肪
     */
    private Double totalFat;

    // ====================== 订单状态 ======================
    /**
     * 订单状态
     * 0：待支付
     * 1：备餐中
     * 2：配送中
     * 3：已完成
     * 5：已取消
     */
    private Integer status;

    // ====================== 时间字段（企业强制） ======================
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    private Long userCouponId;
}