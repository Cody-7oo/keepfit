package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.dto.CartAddDTO;
import com.example.demo.common.dto.CartUpdateDTO;
import com.example.demo.common.vo.CartVO;
import com.example.demo.entity.Cart;

import java.util.List;
import java.util.Map;

public interface CartService extends IService<Cart> {

    // 添加购物车
    void addCart(CartAddDTO dto);

    // 修改数量
    void updateNum(CartUpdateDTO dto);

    // 删除
    void delete(Long id);

    // 我的购物车
    Map<String, Object> myCart(Long userId);
}