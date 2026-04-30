package com.example.demo.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AntiReplay {
    int expire() default 60; // 秒，默认60秒有效
}