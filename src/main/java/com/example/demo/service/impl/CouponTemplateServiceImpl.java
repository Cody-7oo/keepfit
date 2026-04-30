package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.enums.TemplateStatusEnum;
import com.example.demo.entity.CouponTemplate;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.CouponTemplateMapper;
import com.example.demo.service.CouponTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate> implements CouponTemplateService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final String COUPON_TEMPLATE_LIST = "coupon:template:list";

    /**
     * 新增优惠券模板 + 绑定商家ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTemplate(CouponTemplate template, Long merchantId) {
        String lockKey = "coupon:template:add:" + merchantId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                throw new BusinessException(ResultCodeEnum.REPEAT_SUBMIT);
            }

            log.info("[优惠券模板-新增] 商家ID：{}，入参：{}", merchantId, template);
            long start = System.currentTimeMillis();

            // 核心：自动绑定当前商家，禁止伪造
            template.setMerchantId(merchantId);

            template.setStatus(TemplateStatusEnum.ON.getCode());
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());
            boolean save = save(template);
            if (!save) {
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
            }

            redisTemplate.delete(COUPON_TEMPLATE_LIST);
            log.info("[优惠券模板-新增] 成功，清除Redis缓存");

        } catch (BusinessException e) {
            log.warn("[优惠券模板-新增] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[优惠券模板-新增] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 修改优惠券模板 + 权限校验
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(CouponTemplate template, Long merchantId) {
        String lockKey = "coupon:template:update:" + template.getId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                throw new BusinessException(ResultCodeEnum.REPEAT_SUBMIT);
            }

            log.info("[优惠券模板-修改] 商家ID：{}，入参：{}", merchantId, template);
            long start = System.currentTimeMillis();

            // 权限校验：只能修改自己的优惠券
            CouponTemplate exist = getById(template.getId());
            if (exist == null) {
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }
            if (!exist.getMerchantId().equals(merchantId)) {
                throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
            }

            template.setUpdateTime(LocalDateTime.now());
            boolean update = updateById(template);
            if (!update) {
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
            }

            redisTemplate.delete(COUPON_TEMPLATE_LIST);
            log.info("[优惠券模板-修改] 成功，清除Redis缓存");

        } catch (BusinessException e) {
            log.warn("[优惠券模板-修改] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[优惠券模板-修改] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询全部（带 Redis 缓存）
     */
    @Override
    public List<CouponTemplate> listAll() {
        log.info("[优惠券模板-查询全部] 从Redis获取缓存");
        long start = System.currentTimeMillis();
        try {
            List<CouponTemplate> cacheList =
                    (List<CouponTemplate>) redisTemplate.opsForValue().get(COUPON_TEMPLATE_LIST);

            if (cacheList != null && !cacheList.isEmpty()) {
                log.info("[优惠券模板-查询全部] 命中Redis缓存，数量：{}", cacheList.size());
                return cacheList;
            }

            List<CouponTemplate> list = list();
            log.info("[优惠券模板-查询全部] 未命中缓存，查询数据库，数量：{}", list.size());

            redisTemplate.opsForValue().set(COUPON_TEMPLATE_LIST, list, 30, TimeUnit.MINUTES);
            return list;
        } catch (Exception e) {
            log.error("[优惠券模板-查询全部] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[优惠券模板-查询全部] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 根据商家ID查询自己的优惠券
     */
    @Override
    public List<CouponTemplate> listByMerchant(Long merchantId) {
        log.info("[商家-查询优惠券模板列表] 商家ID：{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CouponTemplate::getMerchantId, merchantId);
            return list(wrapper);
        } catch (Exception e) {
            log.error("[商家-查询优惠券模板列表] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商家-查询优惠券模板列表] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}