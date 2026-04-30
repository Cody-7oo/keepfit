package com.example.demo.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流次数
     */
    int limit() default 10;

    /**
     * 限流时间 秒
     */
    int second() default 10;
}