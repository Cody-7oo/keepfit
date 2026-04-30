package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.dto.ProductAddDTO;
import com.example.demo.common.dto.ProductUpdateDTO;
import com.example.demo.common.vo.ProductVO;
import com.example.demo.entity.Product;

import java.util.List;

public interface ProductService extends IService<Product> {

    // ====================== 商家端 ======================
    void addProduct(ProductAddDTO dto);
    void updateProduct(ProductUpdateDTO dto);
    void deleteProduct(Long id);

    // ====================== 用户端 ======================
    List<ProductVO> getUpProductList();

    List<ProductVO> getProductList(Integer category, String keyword);

    IPage<ProductVO> getProductPage(Integer pageNum, Integer pageSize, Integer category, String keyword);
}