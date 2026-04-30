package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.result.R;
import com.example.demo.entity.OrderItem;

import java.util.List;

public interface OrderItemService extends IService<OrderItem> {

    /**
     * 根据订单ID查询该订单所有商品明细
     */
    List<OrderItem> getItemsByOrderId(Long orderId);

    /**
     * 批量保存订单项（下单时使用）
     */
    void saveBatchItems(List<OrderItem> itemList);
}