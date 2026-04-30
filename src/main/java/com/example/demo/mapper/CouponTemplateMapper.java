package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.CacheNamespace;

@CacheNamespace
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {
}