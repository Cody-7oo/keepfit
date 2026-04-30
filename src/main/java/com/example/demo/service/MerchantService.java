package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.dto.MerchantLoginDTO;
import com.example.demo.common.dto.MerchantRegisterDTO;
import com.example.demo.common.vo.MerchantVO;
import com.example.demo.entity.Merchant;

public interface MerchantService extends IService<Merchant> {

    /**
     * 商家注册
     */
    void register(MerchantRegisterDTO dto);

    /**
     * 商家登录
     */
    MerchantVO login(MerchantLoginDTO dto);

    /**
     * 获取商家信息（带缓存）
     */
    MerchantVO getMerchantInfo(Long merchantId);

    /**
     * 修改商家信息（清除缓存）
     */
    void updateMerchant(Merchant merchant);
}