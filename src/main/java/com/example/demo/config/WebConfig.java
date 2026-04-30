package com.example.demo.config;

import com.example.demo.interceptor.SecurityInterceptor;
import com.example.demo.interceptor.IdempotentInterceptor;
import com.example.demo.interceptor.RepeatSubmitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    @Resource
    private IdempotentInterceptor idempotentInterceptor;

    @Resource
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 防重复提交
        registry.addInterceptor(repeatSubmitInterceptor)
                .addPathPatterns("/**");

        // 2. 幂等拦截
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/**");

        // 3. 接口签名 + 防重放（你刚加的安全拦截器）
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**");
    }
}