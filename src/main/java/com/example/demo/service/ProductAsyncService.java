package com.example.demo.service;

import org.springframework.scheduling.annotation.Async;

public interface ProductAsyncService {

    /**
     * 异步清理商品缓存
     */
    @Async("bizAsyncExecutor")
    void clearProductCache(Long productId);

    /**
     * 异步清理商品列表分页缓存
     */
    @Async("bizAsyncExecutor")
    void clearProductListCache();
}