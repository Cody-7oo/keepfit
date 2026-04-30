package com.example.demo.service.impl;

import com.example.demo.service.ProductAsyncService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ProductAsyncServiceImpl implements ProductAsyncService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 你的商品缓存key前缀，和你项目保持一致
    private static final String PRODUCT_DETAIL_PREFIX = "product:detail:";
    private static final String PRODUCT_LIST_PREFIX = "product:list:";

    @Override
    public void clearProductCache(Long productId) {
        // 清空单个商品详情缓存
        redisTemplate.delete(PRODUCT_DETAIL_PREFIX + productId);
    }

    @Override
    public void clearProductListCache() {
        // 模糊删除所有商品列表分页缓存
        var keys = redisTemplate.keys(PRODUCT_LIST_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}