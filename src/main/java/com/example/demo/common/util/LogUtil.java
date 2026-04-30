package com.example.demo.common.util;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.entity.OperationLog;
import com.example.demo.service.OperationLogAsyncService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Component
public class LogUtil {

    @Resource
    private OperationLogAsyncService operationLogAsyncService;

    // 一行记录操作日志
    public void record(String module, String operation) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

            OperationLog log = new OperationLog();
            log.setUserId(StpUtil.getLoginIdAsLong());
            log.setUsername(StpUtil.getSession().getString("username"));
            log.setModule(module);
            log.setOperation(operation);
            log.setRequestUrl(request.getRequestURI());
            log.setRequestMethod(request.getMethod());
            log.setIp(getIp(request));
            log.setCreateTime(LocalDateTime.now());

            // 异步保存！不阻塞业务！
            operationLogAsyncService.saveOperationLog(log);
        } catch (Exception ignored) {}
    }

    // 获取IP
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}