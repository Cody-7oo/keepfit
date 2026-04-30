package com.example.demo.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单返回VO
 * 按需返回、隐藏数据库多余字段
 */
@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private String receiver;
    private String phone;
    private String address;
    private BigDecimal totalPrice;
    private Integer status;
    private Double totalCalorie;
    private Double totalProtein;
    private Double totalCarbohydrate;
    private Double totalFat;
    private LocalDateTime createTime;
}