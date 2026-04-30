package com.example.demo.task;

import com.example.demo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OrderTimeoutTask {

    @Resource
    private OrderService orderService;

    @Resource
    private RedissonClient redissonClient;

    @Value("${order.timeout}")
    private int orderTimeout;

    /**
     * 定时任务 Key（企业规范：必须固定，唯一）
     */
    private static final String LOCK_KEY = "dist:lock:order:timeout:cancel";

    /**
     * 每分钟执行一次：取消超时未支付订单
     * 企业级 Redisson 分布式锁，保证集群环境只执行一次
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void cancelTimeoutOrder() {
        RLock lock = redissonClient.getLock(LOCK_KEY);

        try {
            // 【企业标准】尝试获取锁，不等待；锁持有30秒（防止死锁）
            boolean acquired = lock.tryLock(0, 30, TimeUnit.SECONDS);

            if (!acquired) {
                log.info("[订单超时任务] 其他服务节点已执行，本次跳过");
                return;
            }

            log.info("[订单超时任务] 获取分布式锁成功，开始执行，时间：{}", LocalDateTime.now());

            // 执行业务
            int cancelCount = orderService.cancelTimeoutOrder(orderTimeout);
            log.info("[订单超时任务] 执行完成，取消超时订单数量：{}", cancelCount);

        } catch (InterruptedException e) {
            log.error("[订单超时任务] 获取锁被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("[订单超时任务] 执行异常", e);
        } finally {
            // 【安全规范】只有当前线程持有锁，才释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[订单超时任务] 分布式锁已释放");
            }
        }
    }
}