package com.example.demo.aop;

import com.example.demo.annotation.RateLimit;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

@Slf4j
@Aspect
@Component
@EnableAspectJAutoProxy
@ComponentScan("com.example.demo")
public class RateLimitAop {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private DefaultRedisScript<Long> limitScript;

    @Pointcut("@annotation(com.example.demo.annotation.RateLimit)")
    public void rateLimitPointCut(){}

    @Around("rateLimitPointCut() && @annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getRequest();
        String userId = "anonymous";
        // 你项目如果有登录，从token拿真实userId
        // userId = SecurityUtil.getUserId().toString();

        String uri = request.getRequestURI();
        // 限流key：用户+接口路径，精准限流
        String limitKey = "rate:limit:" + userId + ":" + uri;

        // 调用Lua脚本
        Long result = redisTemplate.execute(
                limitScript,
                Collections.singletonList(limitKey),
                rateLimit.limit(),
                rateLimit.second()
        );

        if (result != null && result == 0) {
            log.warn("[接口限流] 用户:{},接口:{} 访问过于频繁", userId, uri);
            throw new BusinessException(ResultCodeEnum.REQUEST_TOO_FAST);
        }
        return joinPoint.proceed();
    }

    private HttpServletRequest getRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return ((ServletRequestAttributes) attributes).getRequest();
    }
}