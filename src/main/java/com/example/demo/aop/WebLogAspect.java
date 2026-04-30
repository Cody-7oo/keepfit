package com.example.demo.aop;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 全局请求日志 AOP（企业标准）
 */
@Slf4j
@Aspect
@Component
public class WebLogAspect {

    /**
     * 定义切点：所有Controller接口
     */
    @Pointcut("execution(* com.example.demo.controller..*.*(..))")
    public void webLog() {
    }

    /**
     * 环绕通知：记录请求、响应、耗时
     */
    @Around("webLog()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String requestId = IdUtil.fastSimpleUUID();

        // 2. 打印请求日志
        log.info("==================== 请求开始 [{}] ====================", requestId);
        log.info("请求ID    : {}", requestId);
        log.info("请求地址   : {}", request.getRequestURL());
        log.info("请求方式   : {}", request.getMethod());
        log.info("请求IP     : {}", getClientIp(request));
        log.info("请求方法   : {}", joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        log.info("请求参数   : {}", Arrays.toString(joinPoint.getArgs()));

        // 3. 执行接口
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();

        // 4. 打印响应日志
        log.info("请求耗时   : {} ms", endTime - startTime);
        log.info("返回结果   : {}", JSON.toJSONString(result));
        log.info("==================== 请求结束 [{}] ====================\n", requestId);

        return result;
    }

    /**
     * 获取真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}