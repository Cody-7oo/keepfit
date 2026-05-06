package com.example.demo.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.interceptor.SecurityInterceptor;
import com.example.demo.interceptor.IdempotentInterceptor;
import com.example.demo.interceptor.RepeatSubmitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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

    // 全部放行：接口文档 + 登录 + 注册
    private static final String[] EXCLUDE_ALL = {
            "/user/login",
            "/user/register",
            "/merchant/login",
            "/merchant/register",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 🔥 1. 先注册 Sa-Token 拦截器，直接用默认账号校验，先跑通
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_ALL);

        // 2. 然后才是你的自定义拦截器
        registry.addInterceptor(repeatSubmitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_ALL);

        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_ALL);

        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_ALL);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}