package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.dto.OrderCreateDTO;
import com.example.demo.common.vo.OrderVO;
import com.example.demo.entity.Order;

import java.util.List;

public interface OrderService extends IService<Order> {

    // 创建订单 → 返回VO
    OrderVO createOrder(OrderCreateDTO dto);

    // 取消订单 → 无返回
    void cancelOrder(Long orderId, Long userId);

    // 我的订单 → 返回列表
    List<OrderVO> getMyOrder(Long userId);

    // 商家修改订单状态 → 无返回
    void merchantChangeStatus(Long orderId, Integer status, Long merchantId);

    // 商家订单列表 → 返回列表
    List<OrderVO> merchantOrderList(Long merchantId);

    // 定时取消超时订单
    int cancelTimeoutOrder(int timeoutMinutes);
}