package com.example.demo.aop;

import cn.dev33.satoken.stp.StpLogic;
import com.example.demo.annotation.DataScope;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.config.SaTokenConfig;
import com.example.demo.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(dataScope)")
    public void checkDataScope(JoinPoint joinPoint, DataScope dataScope) {
        String scopeType = dataScope.scopeType();

        // 直接获取配置类中注册的 user 账号 StpLogic 实例
        StpLogic stpLogic = new SaTokenConfig().stpUserLogic();
        Long loginId = Long.valueOf(stpLogic.getLoginId().toString());

        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg == null) continue;

            if ("user".equals(scopeType)) {
                Field userIdField = ReflectionUtils.findField(arg.getClass(), "id");
                if (userIdField != null) {
                    userIdField.setAccessible(true);
                    Long dataUserId = (Long) ReflectionUtils.getField(userIdField, arg);
                    if (!loginId.equals(dataUserId)) {
                        throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
                    }
                }
            }
        }
    }
}