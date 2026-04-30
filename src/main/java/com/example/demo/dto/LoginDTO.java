package com.example.demo.dto;

import lombok.Data;

/**
 * 企业标准：登录参数封装
 */
@Data
public class LoginDTO {
    private String phone;
    private String password;
}