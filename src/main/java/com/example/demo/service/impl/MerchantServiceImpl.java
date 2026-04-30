package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.dto.MerchantLoginDTO;
import com.example.demo.common.dto.MerchantRegisterDTO;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.vo.MerchantVO;
import com.example.demo.entity.Merchant;
import com.example.demo.mapper.MerchantMapper;
import com.example.demo.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    /**
     * BCrypt 密码加密器（企业标准，全局单例）
     */
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String MERCHANT_INFO_KEY = "merchant:info:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    /**
     * 商家注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(MerchantRegisterDTO dto) {
        log.info("[商家注册] 手机号：{}", dto.getPhone());
        long start = System.currentTimeMillis();
        try {
            // 1. 校验手机号是否已注册
            LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Merchant::getPhone, dto.getPhone());
            Merchant existMerchant = getOne(queryWrapper);
            if (existMerchant != null) {
                throw new BusinessException(ResultCodeEnum.MERCHANT_PHONE_EXIST);
            }

            // 2. 密码加密（企业规范）
            Merchant merchant = new Merchant();
            BeanUtils.copyProperties(dto, merchant);
            merchant.setPassword(encoder.encode(dto.getPassword()));

            // 3. 保存
            save(merchant);
            log.info("[商家注册] 成功");
        } catch (BusinessException e) {
            log.warn("[商家注册] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[商家注册] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商家注册] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 商家登录
     */
    @Override
    public MerchantVO login(MerchantLoginDTO dto) {
        log.info("[商家登录] 手机号：{}", dto.getPhone());
        long start = System.currentTimeMillis();
        try {
            // 1. 查手机号
            LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Merchant::getPhone, dto.getPhone());
            Merchant merchant = getOne(queryWrapper);

            // 2. 判断是否存在
            if (merchant == null) {
                throw new BusinessException(ResultCodeEnum.MERCHANT_NOT_EXIST);
            }

            // 3. BCrypt 密码校验（企业标准）
            if (!encoder.matches(dto.getPassword(), merchant.getPassword())) {
                throw new BusinessException(ResultCodeEnum.MERCHANT_PASSWORD_ERROR);
            }

            // 4. 转VO
            MerchantVO merchantVO = new MerchantVO();
            BeanUtils.copyProperties(merchant, merchantVO);
            log.info("[商家登录] 成功，商家ID：{}", merchant.getId());
            return merchantVO;
        } catch (BusinessException e) {
            log.warn("[商家登录] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[商家登录] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商家登录] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 查询商家信息（带 Redis 缓存）
     */
    @Override
    public MerchantVO getMerchantInfo(Long merchantId) {
        log.info("[商家-查询信息] ID：{}", merchantId);
        long start = System.currentTimeMillis();
        String cacheKey = MERCHANT_INFO_KEY + merchantId;

        try {
            // 1. 查缓存
            MerchantVO cacheVo = (MerchantVO) redisTemplate.opsForValue().get(cacheKey);
            if (cacheVo != null) {
                log.info("[商家-查询信息] 命中Redis缓存");
                return cacheVo;
            }

            // 2. 查库
            Merchant merchant = getById(merchantId);
            if (merchant == null) {
                throw new BusinessException(ResultCodeEnum.MERCHANT_NOT_EXIST);
            }

            // 3. 转VO
            MerchantVO vo = new MerchantVO();
            BeanUtils.copyProperties(merchant, vo);

            // 4. 存缓存
            redisTemplate.opsForValue().set(cacheKey, vo, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("[商家-查询信息] 存入Redis缓存");
            return vo;
        } catch (BusinessException e) {
            log.warn("[商家-查询信息] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[商家-查询信息] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商家-查询信息] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 修改商家信息（清除缓存）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMerchant(Merchant merchant) {
        log.info("[商家-修改信息] ID：{}", merchant.getId());
        long start = System.currentTimeMillis();
        try {
            updateById(merchant);
            redisTemplate.delete(MERCHANT_INFO_KEY + merchant.getId());
            log.info("[商家-修改信息] 成功，清除缓存");
        } catch (BusinessException e) {
            log.warn("[商家-修改信息] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[商家-修改信息] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商家-修改信息] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}