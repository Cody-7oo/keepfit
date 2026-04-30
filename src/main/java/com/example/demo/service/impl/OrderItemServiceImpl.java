package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.OrderItem;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.service.OrderItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {

    @Resource
    private OrderItemMapper orderItemMapper;

    /**
     * 根据订单ID查询订单项
     */
    @Override
    public List<OrderItem> getItemsByOrderId(Long orderId) {
        log.info("[订单项-查询] 订单ID: {}", orderId);

        return lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .list();
    }

    /**
     * 批量保存订单项
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchItems(List<OrderItem> itemList) {
        log.info("[订单项-批量保存] 商品数量: {}", itemList.size());
        // 🔥 企业级真批量插入（替换原来的 saveBatch）
        orderItemMapper.batchInsert(itemList);
    }
}