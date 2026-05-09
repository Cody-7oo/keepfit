package com.example.demo.interceptor;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.Idempotent;
import com.example.demo.WhiteList;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.enums.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Slf4j // 🔥 加上日志注解
@Component
public class IdempotentInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        log.info("========== 【幂等性拦截器】触发 ==========");
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
            log.info("未登录，直接放行幂等性校验");
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        Idempotent idempotent = method.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            return true;
        }

        // ======================================
        // 🔥 第三步：用对应的 StpLogic 获取 loginId
        // ======================================
        String loginId = stpLogic.getLoginIdAsString();
        log.info("当前登录 ID：{}", loginId);

        // 组装幂等key：用户ID/商家ID + 请求URI
        String key = "idempotent:" + loginId + ":" + uri;
        log.info("幂等性 Key：{}", key);

        // setIfAbsent：不存在才写入，存在直接拒绝
        boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "ok", idempotent.expire(), TimeUnit.SECONDS);

        if (!success) {
            log.warn("【幂等性拦截器】校验失败，重复请求！");
            throw new BusinessException(ResultCodeEnum.IDEMPOTENT_EXPIRE);
        }

        log.info("✅ 幂等性校验通过！");
        return true;
    }
}