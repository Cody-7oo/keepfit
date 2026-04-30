package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.OrderOperationLog;
import com.example.demo.mapper.OrderOperationLogMapper;
import com.example.demo.service.OrderOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单操作日志实现
 */
@Slf4j
@Service
public class OrderOperationLogServiceImpl extends ServiceImpl<OrderOperationLogMapper, OrderOperationLog> implements OrderOperationLogService {

    @Resource
    private OrderOperationLogMapper orderOperationLogMapper;

    /**
     * 保存订单日志
     */
    @Override
    public void saveOrderLog(String orderNo, Long operateUserId, String operateType,
                             String operateContent, String oldStatus, String newStatus, String operateIp) {

        log.info("[订单日志-保存] 订单号：{}，操作人：{}，操作类型：{}", orderNo, operateUserId, operateType);

        OrderOperationLog log = new OrderOperationLog();
        log.setOrderNo(orderNo);
        log.setOperateUserId(operateUserId);
        log.setOperateType(operateType);
        log.setOperateContent(operateContent);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperateIp(operateIp);
        log.setCreateTime(LocalDateTime.now());

        this.save(log);
    }

    /**
     * 根据订单号查询日志
     */
    @Override
    public List<OrderOperationLog> listByOrderNo(String orderNo) {
        LambdaQueryWrapper<OrderOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderOperationLog::getOrderNo, orderNo);
        return this.list(wrapper);
    }
}