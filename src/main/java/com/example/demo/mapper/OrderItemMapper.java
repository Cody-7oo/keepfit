package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import org.apache.ibatis.annotations.CacheNamespace;

@CacheNamespace
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
    /**
     * 企业级真批量插入
     */
    void batchInsert(@Param("list") List<OrderItem> list);

}