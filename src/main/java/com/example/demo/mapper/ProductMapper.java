package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.CacheNamespace;

@CacheNamespace
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    // 不用写任何代码
}