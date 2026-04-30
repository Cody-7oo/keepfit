package com.example.demo.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.DataScope;
import com.example.demo.common.enums.ResultCodeEnum;
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
        // 当前登录ID
        Long loginId = StpUtil.getLoginIdAsLong();
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            // 1. 用户端：校验 userId
            if ("user".equals(scopeType)) {
                Field userIdField = ReflectionUtils.findField(arg.getClass(), "userId");
                if (userIdField != null) {
                    userIdField.setAccessible(true);
                    Long dataUserId = (Long) ReflectionUtils.getField(userIdField, arg);
                    if (!loginId.equals(dataUserId)) {
                        log.error("[数据越权拦截] 登录用户:{} 尝试操作用户:{} 数据", loginId, dataUserId);
                        throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
                    }
                }
            }

            // 2. 商家端：校验 merchantId
            if ("merchant".equals(scopeType)) {
                Field merchantIdField = ReflectionUtils.findField(arg.getClass(), "merchantId");
                if (merchantIdField != null) {
                    merchantIdField.setAccessible(true);
                    Long dataMerchantId = (Long) ReflectionUtils.getField(merchantIdField, arg);
                    if (!loginId.equals(dataMerchantId)) {
                        log.error("[数据越权拦截] 登录商家:{} 尝试操作商家:{} 数据", loginId, dataMerchantId);
                        throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
                    }
                }
            }
        }
    }
}