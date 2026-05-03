package com.example.demo.controller;

import com.example.demo.common.result.R;
import com.example.demo.common.util.RedisMonitorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 可观测性 - 系统监控接口
 * 包含：Redis健康监控、连通性探测
 * 大厂规范：独立监控路由、内部运维使用
 */
@Slf4j
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Resource
    private RedisMonitorUtil redisMonitorUtil;

    /**
     * Redis 健康监控接口
     */
    @GetMapping("/redis/health")
    public R<Map<String, Object>> redisHealth() {
        Map<String, Object> result = new HashMap<>(4);
        boolean connectOk = redisMonitorUtil.isConnectOk();
        result.put("connectStatus", connectOk ? "正常" : "异常");

        if (connectOk) {
            result.put("info", redisMonitorUtil.getRedisInfo());
            log.info("[Redis监控] 连接状态正常");
        } else {
            log.error("[Redis监控] 连接异常，请检查Redis服务配置");
        }
        return R.ok(result);
    }
}