package com.example.demo.common.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Properties;

/**
 * 企业级 Redis 简易监控工具
 * 功能：连通性检测、基础信息采集、运行状态观测
 */
@Component
public class RedisMonitorUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 检测 Redis 连通性
     */
    public boolean isConnectOk() {
        try {
            // 基础心跳命令
            redisTemplate.hasKey("redis:health:ping");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Redis 基础运行信息
     */
    public Properties getRedisInfo() {
        try {
            return redisTemplate.getRequiredConnectionFactory()
                    .getConnection()
                    .info();
        } catch (Exception e) {
            return null;
        }
    }
}