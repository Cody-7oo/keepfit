package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.dto.CartAddDTO;
import com.example.demo.common.dto.CartUpdateDTO;
import com.example.demo.entity.Cart;

import java.util.Map;

public interface CartService extends IService<Cart> {

    /**
     * 添加购物车
     */
    void addCart(CartAddDTO dto);

    /**
     * 修改商品数量（+/- 核心接口）
     */
    void updateNum(CartUpdateDTO dto);

    /**
     * 删除购物车中的指定商品
     */
    void deleteProduct(Long userId, Long productId);

    /**
     * 我的购物车（包含商品列表+营养统计）
     */
    Map<String, Object> myCart(Long userId);

    /**
     * 下单后清空用户购物车
     */
    void clearUserCart(Long userId);
}