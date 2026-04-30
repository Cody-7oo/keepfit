package com.example.demo.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handler -> {
            SaRouter
                    .match("/api/**") // 拦截所有 /api/ 开头的接口
                    .notMatch("/api/login")      // 放行登录
                    .notMatch("/api/register")   // 放行注册
                    .notMatch("/api/checkLogin") // 放行检查登录
                    .notMatch("/api/logout")     // 放行退出登录
                    .check(r -> StpUtil.checkLogin()); // 其他必须登录
        })).addPathPatterns("/**");
    }
}