package com.example.demo.common.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Data
public class ProductQueryDTO {

    /**
     * 分类ID（允许为空，不强制）
     */
    private Long categoryId;

    /**
     * 搜索关键词（最多50字符，防止过长参数）
     */
    @Size(max = 50, message = "搜索关键词长度不能超过50个字符")
    private String keyword;

    /**
     * 当前页码，最小为1
     */
    @Min(value = 1, message = "当前页码不能小于1")
    private Long current;

    /**
     * 每页条数，限制1~50条
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Size(max = 50, message = "每页条数最多50条")
    private Long size;
}