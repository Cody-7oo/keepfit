package com.example.demo.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.Idempotent;
import com.example.demo.WhiteList;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.enums.ResultCodeEnum;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Component
public class IdempotentInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // ====================== 🔥 放行接口文档（最关键！） ======================
        String uri = request.getRequestURI();
        if (WhiteList.isDocUrl(uri)) {
            return true;
        }

        // 只拦截Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod method = (HandlerMethod) handler;
        Idempotent idempotent = method.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            return true;
        }

        // 组装幂等key：用户ID/商家ID + 请求URI
        String loginId = StpUtil.getLoginIdAsString();
        String key = "idempotent:" + loginId + ":" + request.getRequestURI();

        // setIfAbsent：不存在才写入，存在直接拒绝
        boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "ok", idempotent.expire(), TimeUnit.SECONDS);

        if (!success) {
            throw new BusinessException(ResultCodeEnum.IDEMPOTENT_EXPIRE);
        }
        return true;
    }
}