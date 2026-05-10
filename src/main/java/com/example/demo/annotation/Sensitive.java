package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * 敏感信息加密注解（标记在实体类字段上）
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sensitive {
}