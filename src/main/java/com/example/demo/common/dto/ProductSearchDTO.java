package com.example.demo.common.dto;

import lombok.Data;

/**
 * 用户端商品搜索 DTO
 * 前端传递JSON格式参数
 */
@Data
public class ProductSearchDTO {

    /**
     * 分类ID
     */
    private Integer category;

    /**
     * 搜索关键词
     */
    private String keyword;
}