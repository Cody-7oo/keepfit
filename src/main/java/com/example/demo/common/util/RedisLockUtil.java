package com.example.demo.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 * 企业规范：定时任务、并发扣库存、幂等场景专用
 */
@Slf4j
@Component
public class RedisLockUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 分布式锁Key前缀
    private static final String LOCK_PREFIX = "dist:lock:";

    /**
     * 加锁
     * @param lockKey 锁标识
     * @param expireSecond 锁过期时间（防止死锁）
     * @return true=加锁成功
     */
    public boolean tryLock(String lockKey, long expireSecond) {
        String key = LOCK_PREFIX + lockKey;
        // setIfAbsent = NX 互斥机制
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(key, "locked", expireSecond, TimeUnit.SECONDS)
        );
    }

    /**
     * 释放锁
     * @param lockKey 锁标识
     */
    public void unLock(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.error("分布式锁释放异常，key:{}", lockKey, e);
        }
    }
}