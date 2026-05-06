package com.example.demo.task;

import com.example.demo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Slf4j
@Component
public class OrderTimeoutTask {

    @Resource
    private OrderService orderService;

    @Value("${order.timeout}")
    private int orderTimeout;

    /**
     * 每分钟执行一次：取消超时未支付订单
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void cancelTimeoutOrder() {
        try {
            log.info("[订单超时任务] 开始执行，时间：{}", LocalDateTime.now());

            // 执行业务
            int cancelCount = orderService.cancelTimeoutOrder(orderTimeout);
            log.info("[订单超时任务] 执行完成，取消超时订单数量：{}", cancelCount);

        } catch (Exception e) {
            log.error("[订单超时任务] 执行异常", e);
        }
    }
}