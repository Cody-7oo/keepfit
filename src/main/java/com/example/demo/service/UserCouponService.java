package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.dto.CouponQueryDTO;
import com.example.demo.common.dto.CouponUseDTO;
import com.example.demo.common.vo.CouponVO;
import com.example.demo.entity.UserCoupon;
import java.util.List;

public interface UserCouponService extends IService<UserCoupon> {

    /**
     * 赠送咖啡优惠券
     */
    void grantCoffeeCoupon(Long userId);

    /**
     * 我的优惠券列表
     */
    List<CouponVO> myCoupons(CouponQueryDTO dto);

    /**
     * 使用优惠券
     */
    void useCoupon(CouponUseDTO dto);

    /**
     * 检查是否有未使用的咖啡券
     */
    boolean checkUserHasUnusedCoffeeCoupon(CouponQueryDTO dto);
}