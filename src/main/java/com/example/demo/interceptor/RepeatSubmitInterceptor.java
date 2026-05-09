package com.example.demo.interceptor;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.RepeatSubmit;
import com.example.demo.WhiteList;
import com.example.demo.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Slf4j // 🔥 加上日志注解
@Component
public class RepeatSubmitInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        log.info("========== 【防重复提交拦截器】触发 ==========");
        log.info("当前请求 URI：{}", uri);

        if (WhiteList.isDocUrl(uri)) {
            return true;
        }

        // 只拦截Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // ======================================
        // 🔥 第一步：自动识别是用户还是商家
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
        // 🔥 第二步：用对应的 StpLogic 判断登录
        // ======================================
        if (!stpLogic.isLogin()) {
            log.info("未登录，直接放行防重复提交校验");
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RepeatSubmit annotation = handlerMethod.getMethodAnnotation(RepeatSubmit.class);

        // 接口没有加 @RepeatSubmit 就放行
        if (annotation == null) {
            return true;
        }

        // ======================================
        // 🔥 第三步：用对应的 StpLogic 获取 loginId
        // ======================================
        String loginId = stpLogic.getLoginIdAsString();
        log.info("当前登录 ID：{}", loginId);

        // 拼接Redis的key：用户ID/商家ID + 请求地址
        String key = "repeat:submit:" + loginId + ":" + uri;
        log.info("防重复提交 Key：{}", key);

        // 如果redis里有这个key → 重复提交
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            log.warn("【防重复提交拦截器】校验失败，重复提交！");
            throw new BusinessException(400, "请勿重复提交");
        }

        // 存入redis，设置过期时间（默认1秒）
        redisTemplate.opsForValue().set(key, "1", annotation.expire(), TimeUnit.SECONDS);
        log.info("✅ 防重复提交校验通过！");
        return true;
    }
}