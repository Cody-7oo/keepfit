package com.example.demo.common.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductVO {
    private Long id;
    private String productName;
    private String productImg;
    private BigDecimal price;
    private Long categoryId;
    private Double calorie;
    private Double protein;
    private Double carbohydrate;
    private Double fat;
    private String description;
}