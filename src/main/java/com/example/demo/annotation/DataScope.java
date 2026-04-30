package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解 - 防水平越权
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 数据隔离类型：user / merchant
     */
    String scopeType();
}