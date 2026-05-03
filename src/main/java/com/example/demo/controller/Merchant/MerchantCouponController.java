package com.example.demo.controller.Merchant;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.result.R;
import com.example.demo.entity.CouponTemplate;
import com.example.demo.service.CouponTemplateService;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/merchant/coupon")
@SaCheckLogin(type = "merchant")
//@Api(tags = "商家 - 优惠券管理")
public class MerchantCouponController {

    @Resource
    private CouponTemplateService couponTemplateService;

    /**
     * 新增优惠券模板
     */
    @Idempotent
    @RepeatSubmit
    @SaCheckLogin(type = "merchant")
    @SaCheckPermission("merchant:coupon:add")
    @PostMapping("/add")
//    @ApiOperation("新增优惠券模板")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "merchant")
    @ApiSignature
    @AntiReplay
    public R<Void> add(@RequestBody @Valid CouponTemplate template) {
        Long merchantId = StpUtil.getLoginIdAsLong();
        log.info("[商家-新增优惠券] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            couponTemplateService.addTemplate(template, merchantId);
            log.info("[业务埋点-优惠券模板新增成功] merchantId:{}, couponName:{}, couponType:{}, deductPrice:{}",
                    merchantId, template.getCouponName(), template.getCouponType(), template.getDeductPrice());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-优惠券模板新增失败] merchantId:{}, couponName:{}, 失败原因:{}",
                    merchantId, template.getCouponName(), e.getMessage());
            log.error("[商家-新增优惠券] 异常", e);
            throw e;
        } finally {
            log.info("[商家-新增优惠券] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 修改优惠券模板
     */
    @Idempotent
    @RepeatSubmit
    @SaCheckLogin(type = "merchant")
    @SaCheckPermission("merchant:coupon:update")
    @PostMapping("/update")
//    @ApiOperation("修改优惠券模板")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "merchant")
    @ApiSignature
    @AntiReplay
    public R<Void> update(@RequestBody @Valid CouponTemplate template) {
        Long merchantId = StpUtil.getLoginIdAsLong();
        log.info("[商家-修改优惠券] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            CouponTemplate old = couponTemplateService.getById(template.getId());
            if (old == null) {
                throw new BusinessException(ResultCodeEnum.COUPON_TEMPLATE_NOT_EXIST);
            }

            couponTemplateService.updateTemplate(template, merchantId);
            log.info("[业务埋点-优惠券模板修改成功] merchantId:{}, id:{}, oldDeductPrice:{}, newDeductPrice:{}, oldStatus:{}, newStatus:{}",
                    merchantId, template.getId(),
                    old.getDeductPrice(), template.getDeductPrice(),
                    old.getStatus(), template.getStatus());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-优惠券模板修改失败] merchantId:{}, id:{}, 失败原因:{}",
                    merchantId, template.getId(), e.getMessage());
            log.error("[商家-修改优惠券] 异常", e);
            throw e;
        } finally {
            log.info("[商家-修改优惠券] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 查询优惠券列表
     */
    @GetMapping("/list")
//    @ApiOperation("查询商家优惠券列表")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "merchant")
    @ApiSignature
    @AntiReplay
    public R<?> list() {
        Long merchantId = StpUtil.getLoginIdAsLong();
        log.info("[商家-查询优惠券] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            return R.ok(couponTemplateService.listByMerchant(merchantId));
        } catch (Exception e) {
            log.error("[商家-查询优惠券] 异常", e);
            throw e;
        } finally {
            log.info("[商家-查询优惠券] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}