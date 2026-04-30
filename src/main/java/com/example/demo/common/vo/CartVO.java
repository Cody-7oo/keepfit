package com.example.demo.common.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartVO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImg;
    private BigDecimal price;
    private Integer num;

    // 营养字段
    private Double calorie;
    private Double protein;
    private Double carbohydrate;
    private Double fat;
}