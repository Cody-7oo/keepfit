package com.example.demo.aop;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;

@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(dataScope)")
    public void checkDataScope(JoinPoint joinPoint, DataScope dataScope) {
        String scopeType = dataScope.scopeType();
        log.info("========== 【DataScope切面】触发 ==========");
        log.info("当前 scopeType：{}", scopeType);

        // ======================================
        // 🔥 第一步：获取当前请求的 URI
        // ======================================
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String uri = request.getRequestURI();
        log.info("当前请求 URI：{}", uri);

        // ======================================
        // 🔥 第二步：自动识别是用户还是商家
        // ======================================
        StpLogic stpLogic;
        if (uri.startsWith("/merchant/")) {
            stpLogic = SaManager.getStpLogic("merchant");
            log.info("当前使用 StpLogic 类型：merchant");
        } else {
            stpLogic = StpUtil.stpLogic;
            log.info("当前使用 StpLogic 类型：user");
        }

        // ======================================
        // 🔥 第三步：获取登录 ID
        // ======================================
        Long loginId = Long.valueOf(stpLogic.getLoginId().toString());
        log.info("当前登录 ID：{}", loginId);

        // ======================================
        // 🔥 第四步：根据 scopeType 进行权限校验
        // ======================================
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg == null) continue;

            // 处理 user 类型
            if ("user".equals(scopeType)) {
                Field userIdField = ReflectionUtils.findField(arg.getClass(), "id");
                if (userIdField != null) {
                    userIdField.setAccessible(true);
                    Long dataUserId = (Long) ReflectionUtils.getField(userIdField, arg);
                    log.info("数据中的 user ID：{}", dataUserId);

                    if (!loginId.equals(dataUserId)) {
                        log.warn("【DataScope】权限校验失败！登录ID：{}，数据ID：{}", loginId, dataUserId);
                        throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
                    }
                    log.info("【DataScope】user 权限校验通过！");
                }
            }

            // ======================================
            // 🔥 新增：处理 merchant 类型
            // ======================================
            if ("merchant".equals(scopeType)) {
                // 先尝试找 merchantId 字段
                Field merchantIdField = ReflectionUtils.findField(arg.getClass(), "merchantId");
                if (merchantIdField == null) {
                    // 如果没有 merchantId，尝试找 id 字段
                    merchantIdField = ReflectionUtils.findField(arg.getClass(), "id");
                }

                if (merchantIdField != null) {
                    merchantIdField.setAccessible(true);
                    Long dataMerchantId = (Long) ReflectionUtils.getField(merchantIdField, arg);
                    log.info("数据中的 merchant ID：{}", dataMerchantId);

                    if (!loginId.equals(dataMerchantId)) {
                        log.warn("【DataScope】权限校验失败！登录ID：{}，数据ID：{}", loginId, dataMerchantId);
                        throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
                    }
                    log.info("【DataScope】merchant 权限校验通过！");
                }
            }
        }
    }
}