package com.example.demo.controller.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.dto.CouponQueryDTO;
import com.example.demo.common.dto.CouponUseDTO;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.CouponVO;
import com.example.demo.entity.UserCoupon;
import com.example.demo.service.UserCouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/coupon")
@SaCheckLogin(type = "user")
public class UserCouponController {

    @Resource
    private UserCouponService userCouponService;

    private Long getLoginUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @GetMapping("/my")
    public R<List<CouponVO>> myCoupons(@Valid CouponQueryDTO dto) {
        Long userId = getLoginUserId();
        dto.setUserId(userId);

        log.info("[用户-我的优惠券] 用户ID：{}", userId);
        long start = System.currentTimeMillis();
        try {
            return R.ok(userCouponService.myCoupons(dto));
        } catch (Exception e) {
            log.error("[用户-我的优惠券] 异常：", e);
            throw e;
        } finally {
            log.info("[用户-我的优惠券] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @PostMapping("/use")
    public R<Void> useCoupon(@RequestBody @Valid CouponUseDTO dto) {
        Long userId = getLoginUserId();
        dto.setUserId(userId);

        log.info("[用户-使用优惠券] 用户ID：{}，券ID：{}", userId, dto.getUserCouponId());
        long start = System.currentTimeMillis();
        try {
            UserCoupon userCoupon = userCouponService.getById(dto.getUserCouponId());
            if (userCoupon == null) {
                throw new BusinessException(ResultCodeEnum.COUPON_NOT_EXIST);
            }

            userCouponService.useCoupon(dto);
            log.info("[业务埋点-优惠券核销成功] userId:{}, userCouponId:{}, orderNo:{}",
                    userId, dto.getUserCouponId(), dto.getOrderNo());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-优惠券核销异常] userId:{}, userCouponId:{}, 异常原因:{}",
                    userId, dto.getUserCouponId(), e.getMessage());
            log.error("[用户-使用优惠券] 异常：", e);
            throw e;
        } finally {
            log.info("[用户-使用优惠券] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @GetMapping("/hasCoffee")
    public R<Boolean> hasCoffee(@Valid CouponQueryDTO dto) {
        Long userId = getLoginUserId();
        dto.setUserId(userId);

        log.info("[用户-查询是否有咖啡券] 用户ID：{}", userId);
        long start = System.currentTimeMillis();
        try {
            boolean has = userCouponService.checkUserHasUnusedCoffeeCoupon(dto);
            log.info("[业务埋点-咖啡券查询] userId:{}, 是否有可用券:{}", userId, has);
            return R.ok(has);
        } catch (Exception e) {
            log.error("[用户-查询是否有咖啡券] 异常：", e);
            throw e;
        } finally {
            log.info("[用户-查询是否有咖啡券] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}