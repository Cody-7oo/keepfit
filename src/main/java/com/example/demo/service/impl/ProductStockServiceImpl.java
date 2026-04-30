package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.entity.ProductStock;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.ProductStockMapper;
import com.example.demo.service.ProductStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 商品库存Service实现
 */
@Slf4j
@Service
public class ProductStockServiceImpl extends ServiceImpl<ProductStockMapper, ProductStock> implements ProductStockService {

    @Resource
    private ProductStockMapper productStockMapper;

    /**
     * 扣减库存
     */
    @Override
    public void deductStock(Long productId, Integer num) {
        log.info("[库存-扣减] 商品ID：{}，扣减数量：{}", productId, num);

        ProductStock stock = this.getByProductId(productId);
        if (stock == null) {
            throw new BusinessException(ResultCodeEnum.STOCK_NOT_ENOUGH);
        }

        if (stock.getStock() < num) {
            throw new BusinessException(ResultCodeEnum.STOCK_NOT_ENOUGH);
        }

        stock.setStock(stock.getStock() - num);
        this.updateById(stock);

        log.info("[库存-扣减] 扣减成功，剩余库存：{}", stock.getStock());
    }

    /**
     * 回滚库存
     */
    @Override
    public void rollbackStock(Long productId, Integer num) {
        log.info("[库存-回滚] 商品ID：{}，回滚数量：{}", productId, num);

        ProductStock stock = this.getByProductId(productId);
        if (stock == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }

        stock.setStock(stock.getStock() + num);
        this.updateById(stock);

        log.info("[库存-回滚] 回滚成功，当前库存：{}", stock.getStock());
    }

    /**
     * 根据商品ID查询库存
     */
    @Override
    public ProductStock getByProductId(Long productId) {
        LambdaQueryWrapper<ProductStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductStock::getProductId, productId);
        return this.getOne(wrapper);
    }
}