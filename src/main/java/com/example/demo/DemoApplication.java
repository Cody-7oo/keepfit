package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableScheduling
@EnableAsync
@ServletComponentScan
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
})
@MapperScan("com.example.demo.mapper")
@EnableTransactionManagement
public class DemoApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DemoApplication.class);
        app.setRegisterShutdownHook(true);
        app.run(args);

        System.out.println("✅ 项目启动成功 ");
        System.out.println("📖 文档地址：http://localhost:8080/swagger-ui/index.html");
    }

    // ======================
    // 🔥 关键：这里【空】！！！
    // 不注册任何拦截器！！！
    // ======================
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 什么都不写！
    }
}