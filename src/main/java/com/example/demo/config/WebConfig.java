package com.example.demo.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.interceptor.IdempotentInterceptor;
import com.example.demo.interceptor.RepeatSubmitInterceptor;
import com.example.demo.interceptor.SecurityInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    @Resource
    private IdempotentInterceptor idempotentInterceptor;

    @Resource
    private SecurityInterceptor securityInterceptor;

    // 放行：登录、注册、接口文档
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
        // ======================================
        // 1. 用户端拦截器（修复编译报错，带调试日志）
        // ======================================
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 用 Spring 原生方式获取请求，100% 兼容
                    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

                    log.info("========== 【用户端拦截器】触发 ==========");
                    log.info("当前请求 URI：{}", request.getRequestURI());
                    log.info("当前使用 StpLogic 类型：user");

                    StpUtil.checkLogin();

                    log.info("✅ 用户端 token 校验通过！");
                    log.info("当前登录用户 ID：{}", StpUtil.getLoginIdAsString());
                }))
                .addPathPatterns("/user/**")
                .excludePathPatterns(EXCLUDE_ALL);

        // ======================================
        // 2. 商家端拦截器（修复编译报错，带调试日志）
        // ======================================
        registry.addInterceptor(new SaInterceptor(handle -> {
                    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

                    log.info("========== 【商家端拦截器】触发 ==========");
                    log.info("当前请求 URI：{}", request.getRequestURI());
                    log.info("当前使用 StpLogic 类型：merchant");

                    StpLogic merchantStp = SaManager.getStpLogic("merchant");
                    merchantStp.checkLogin();

                    log.info("✅ 商家端 token 校验通过！");
                    log.info("当前登录商家 ID：{}", merchantStp.getLoginIdAsString());
                }))
                .addPathPatterns("/merchant/**")
                .excludePathPatterns(EXCLUDE_ALL);

        // 3. 防重复提交拦截器（不变）
        registry.addInterceptor(repeatSubmitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_ALL);

        // 4. 幂等性拦截器（不变）
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_ALL);

        // 5. 签名安全校验拦截器（不变）
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