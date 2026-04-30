package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 * 企业规范：统一跨域，禁止注解零散配置
 * 适配前后端分离、Cookie、Token、Sa-Token
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许所有来源（生产环境可指定前端域名）
                .allowedOriginPatterns("*")
                // 允许携带 Cookie / Token 凭证
                .allowCredentials(true)
                // 允许请求方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许所有请求头
                .allowedHeaders("*")
                // 预检请求有效期 1小时
                .maxAge(3600);
    }
}