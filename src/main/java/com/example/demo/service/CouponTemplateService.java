package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.CouponTemplate;

import java.util.List;

public interface CouponTemplateService extends IService<CouponTemplate> {

    /**
     * 新增优惠券模板
     */
    void addTemplate(CouponTemplate template, Long merchantId);

    /**
     * 修改优惠券模板
     */
    void updateTemplate(CouponTemplate template, Long merchantId);

    /**
     * 查询全部优惠券
     */
    List<CouponTemplate> listAll();

    /**
     * 根据商家ID查询优惠券
     */
    List<CouponTemplate> listByMerchant(Long merchantId);
}