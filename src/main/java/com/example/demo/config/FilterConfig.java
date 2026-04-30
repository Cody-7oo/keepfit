package com.example.demo.config;

import com.example.demo.filter.SqlInjectionFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<SqlInjectionFilter> sqlInjectionFilterRegistration() {
        FilterRegistrationBean<SqlInjectionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SqlInjectionFilter());
        registration.addUrlPatterns("/*");
        registration.setName("sqlInjectionFilter");
        registration.setOrder(1);
        return registration;
    }
}