package com.example.demo.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.example.demo.annotation.AntiReplay;
import com.example.demo.annotation.ApiSignature;
import com.example.demo.WhiteList;
import com.example.demo.common.constant.SecurityConstant;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.enums.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SecurityInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String uri = request.getRequestURI();
        if (WhiteList.isDocUrl(uri)) {
            return true;
        }

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod method = (HandlerMethod) handler;

        // 防止Redis空指针
        if (redisTemplate == null) {
            log.warn("Redis 未连接，跳过防重放与签名");
            return true;
        }

        // ====================== 1. 防重放 ======================
        AntiReplay antiReplay = method.getMethodAnnotation(AntiReplay.class);
        if (antiReplay != null) {
            String nonce = request.getHeader("nonce");
            String timestamp = request.getHeader("timestamp");

            if (StrUtil.isBlank(nonce) || StrUtil.isBlank(timestamp)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "缺少重放防护参数");
            }

            long now = System.currentTimeMillis() / 1000;
            long ts = Long.parseLong(timestamp);
            if (Math.abs(now - ts) > antiReplay.expire()) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "请求已过期");
            }

            if (Boolean.TRUE.equals(redisTemplate.hasKey("replay:" + nonce))) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "请勿重复请求");
            }
            redisTemplate.opsForValue().set("replay:" + nonce, "1", antiReplay.expire(), TimeUnit.SECONDS);
        }

        // ====================== 2. 接口签名 ======================
        ApiSignature signature = method.getMethodAnnotation(ApiSignature.class);
        if (signature != null) {
            String timestamp = request.getHeader("timestamp");
            String nonce = request.getHeader("nonce");
            String sign = request.getHeader("sign");

            if (StrUtil.isBlank(timestamp) || StrUtil.isBlank(nonce) || StrUtil.isBlank(sign)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "缺少签名参数");
            }

            Map<String, String> params = new TreeMap<>();
            Map<String, String[]> p = request.getParameterMap();
            for (String key : p.keySet()) {
                params.put(key, Arrays.toString(p.get(key)));
            }
            params.put("timestamp", timestamp);
            params.put("nonce", nonce);

            StringBuilder sb = new StringBuilder();
            params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
            String str = sb.substring(0, sb.length() - 1);

            HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, SecurityConstant.API_SECRET.getBytes());
            String serverSign = hMac.digestHex(str);

            if (!serverSign.equalsIgnoreCase(sign)) {
                log.error("签名错误，原文：{}，服务端：{}，客户端：{}", str, serverSign, sign);
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "签名错误");
            }
        }

        return true;
    }
}