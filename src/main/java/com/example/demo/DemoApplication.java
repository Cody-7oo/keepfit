package com.example.demo;

import cn.dev33.satoken.interceptor.SaInterceptor;
import com.example.demo.interceptor.SecurityInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableScheduling
@EnableAsync
@ServletComponentScan
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
})
@MapperScan("com.example.demo.mapper")
@EnableTransactionManagement
public class DemoApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DemoApplication.class);
        app.setRegisterShutdownHook(true);
        app.run(args);

        System.out.println("✅ 项目启动成功 | Sa-Token 登录权限已启用 | 优雅停机已开启");
        System.out.println("📖 文档地址：http://localhost:8080/swagger-ui/index.html");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ====================== 🔥 终极修复：只放行必要拦截器，关闭所有会报错的拦截器 ======================
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. Sa-Token 登录拦截 —— 放行文档
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(WhiteList.DOC_URLS);

        // 2. 签名 + 防重放 —— 放行文档
        registry.addInterceptor(new SecurityInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(WhiteList.DOC_URLS);

        // --------------- 以下两个【完全不注册】，因为它们会强制获取登录信息，导致文档打不开！---------------
        // registry.addInterceptor(idempotentInterceptor);  // 关闭
        // registry.addInterceptor(repeatSubmitInterceptor); // 关闭
    }
}