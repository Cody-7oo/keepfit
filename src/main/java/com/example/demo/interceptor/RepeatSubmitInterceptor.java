package com.example.demo.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.RepeatSubmit;
import com.example.demo.WhiteList;
import com.example.demo.exception.BusinessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Component
public class RepeatSubmitInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // ====================== 🔥 放行文档！必须加！======================
        String uri = request.getRequestURI();
        if (WhiteList.isDocUrl(uri)) {
            return true;
        }

        // 只拦截Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RepeatSubmit annotation = handlerMethod.getMethodAnnotation(RepeatSubmit.class);

        // 接口没有加 @RepeatSubmit 就放行
        if (annotation == null) {
            return true;
        }

        // 拼接Redis的key：用户ID + 请求地址
        String key = "repeat:submit:" + StpUtil.getLoginIdAsString() + ":" + request.getRequestURI();

        // 如果redis里有这个key → 重复提交
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            throw new BusinessException(400, "请勿重复提交");
        }

        // 存入redis，设置过期时间（默认1秒）
        redisTemplate.opsForValue().set(key, "1", annotation.expire(), TimeUnit.SECONDS);
        return true;
    }
}