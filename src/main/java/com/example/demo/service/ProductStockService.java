package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.ProductStock;

/**
 * 商品库存Service
 * 负责商品库存的扣减、回滚、查询等核心业务操作
 */
public interface ProductStockService extends IService<ProductStock> {

    // 扣减商品库存（下单时调用，扣减可用库存，库存不足则抛异常）
    void deductStock(Long productId, Integer num);

    // 回滚商品库存（订单取消/关闭时调用，归还扣减的库存）
    void rollbackStock(Long productId, Integer num);

    // 根据商品ID查询库存信息（用于下单前库存校验）
    ProductStock getByProductId(Long productId);
}