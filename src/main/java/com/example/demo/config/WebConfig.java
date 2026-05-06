package com.example.demo.config;

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

    // 🔥 全部放行：接口文档 + 登录 + 注册
    private static final String[] EXCLUDE_ALL = {
            "/user/login",
            "/user/register",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 防重复提交
        registry.addInterceptor(repeatSubmitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_ALL);

        // 2. 幂等拦截
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_ALL);

        // 3. 接口签名 + 防重放
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_ALL);
    }

    // 放行静态资源
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}