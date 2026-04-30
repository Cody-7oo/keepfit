package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.OrderOperationLog;

import java.util.List;

/**
 * 订单操作日志Service
 * 负责订单全生命周期操作日志的记录、查询，用于审计和问题排查
 */
public interface OrderOperationLogService extends IService<OrderOperationLog> {

    // 保存订单操作日志（记录操作人、操作类型、订单状态变更等关键信息）
    void saveOrderLog(String orderNo, Long operateUserId, String operateType, String operateContent, String oldStatus, String newStatus, String operateIp);

    // 根据订单号查询操作日志列表（用于订单操作追溯）
    List<OrderOperationLog> listByOrderNo(String orderNo);
}