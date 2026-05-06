package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.dto.CouponQueryDTO;
import com.example.demo.common.dto.CouponUseDTO;
import com.example.demo.common.enums.CouponTypeEnum;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.enums.UserCouponStatusEnum;
import com.example.demo.common.vo.CouponVO;
import com.example.demo.entity.CouponTemplate;
import com.example.demo.entity.UserCoupon;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.UserCouponMapper;
import com.example.demo.service.CouponTemplateService;
import com.example.demo.service.UserCouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements UserCouponService {

    @Resource
    private CouponTemplateService couponTemplateService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_COUPON_KEY = "user:coupon:";

    /**
     * 发放咖啡券
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantCoffeeCoupon(Long userId) {
        log.info("[优惠券-发放咖啡券] 用户ID：{}", userId);
        long start = System.currentTimeMillis();

        try {
            LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CouponTemplate::getCouponType, CouponTypeEnum.COFFEE_COUPON.getCode());
            CouponTemplate template = couponTemplateService.getOne(wrapper);

            if (template == null) {
                throw new BusinessException(ResultCodeEnum.COUPON_TEMPLATE_NOT_EXIST);
            }

            UserCoupon uc = new UserCoupon();
            uc.setUserId(userId);
            uc.setTemplateId(template.getId());
            uc.setStatus(UserCouponStatusEnum.UN_USED.getCode());
            uc.setReceiveTime(LocalDateTime.now());
            uc.setExpireTime(LocalDateTime.now().plusDays(template.getValidDay()));
            uc.setMerchantId(1L);
            save(uc);

            redisTemplate.delete(USER_COUPON_KEY + userId);
            log.info("[优惠券-发放咖啡券] 成功，清除缓存");

        } catch (BusinessException e) {
            log.warn("[优惠券-发放咖啡券] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[优惠券-发放咖啡券] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 查询我的优惠券
     */
    @Override
    public List<CouponVO> myCoupons(CouponQueryDTO dto) {
        log.info("[优惠券-我的优惠券] 用户ID：{}", dto.getUserId());
        long start = System.currentTimeMillis();
        try {
            String cacheKey = USER_COUPON_KEY + dto.getUserId();

            List<CouponVO> cacheList = (List<CouponVO>) redisTemplate.opsForValue().get(cacheKey);
            if (cacheList != null) {
                log.info("[优惠券-我的优惠券] 命中Redis缓存，数量：{}", cacheList.size());
                return cacheList;
            }

            LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserCoupon::getUserId, dto.getUserId());
            wrapper.orderByDesc(UserCoupon::getReceiveTime);
            List<UserCoupon> list = list(wrapper);

            List<CouponVO> voList = list.stream().map(item -> {
                CouponVO vo = new CouponVO();
                BeanUtils.copyProperties(item, vo);
                return vo;
            }).collect(Collectors.toList());

            redisTemplate.opsForValue().set(cacheKey, voList, 10, TimeUnit.MINUTES);
            log.info("[优惠券-我的优惠券] 查询数据库，数量：{}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("[优惠券-我的优惠券] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[优惠券-我的优惠券] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 使用优惠券
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(CouponUseDTO dto) {
        log.info("[优惠券-使用] 用户ID：{}，券ID：{}", dto.getUserId(), dto.getUserCouponId());
        long start = System.currentTimeMillis();

        try {
            UserCoupon uc = getById(dto.getUserCouponId());
            if (uc == null) {
                throw new BusinessException(ResultCodeEnum.COUPON_NOT_EXIST);
            }
            if (!uc.getUserId().equals(dto.getUserId())) {
                throw new BusinessException(ResultCodeEnum.COUPON_NO_PERMISSION);
            }
            if (!uc.getStatus().equals(UserCouponStatusEnum.UN_USED.getCode())) {
                throw new BusinessException(ResultCodeEnum.COUPON_USED_OR_EXPIRE);
            }
            if (uc.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException(ResultCodeEnum.COUPON_USED_OR_EXPIRE);
            }

            uc.setStatus(UserCouponStatusEnum.USED.getCode());
            uc.setUseTime(LocalDateTime.now());
            updateById(uc);

            redisTemplate.delete(USER_COUPON_KEY + dto.getUserId());
            log.info("[优惠券-使用] 成功，清除缓存");

        } catch (BusinessException e) {
            log.warn("[优惠券-使用] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[优惠券-使用] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 检查是否有未使用咖啡券
     */
    @Override
    public boolean checkUserHasUnusedCoffeeCoupon(CouponQueryDTO dto) {
        log.info("[优惠券-检查咖啡券] 用户ID：{}", dto.getUserId());
        try {
            LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserCoupon::getUserId, dto.getUserId());
            wrapper.eq(UserCoupon::getStatus, UserCouponStatusEnum.UN_USED.getCode());
            long count = count(wrapper);
            log.info("[优惠券-检查咖啡券] 未使用数量：{}", count);
            return count > 0;
        } catch (Exception e) {
            log.error("[优惠券-检查咖啡券] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }
}