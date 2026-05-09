package com.example.demo.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

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
            // 校验手机号是否已注册
            LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Merchant::getPhone, dto.getPhone());
            Merchant existMerchant = getOne(queryWrapper);
            if (existMerchant != null) {
                throw new BusinessException(ResultCodeEnum.MERCHANT_PHONE_EXIST);
            }

            // 自动拷贝：phone, merchantName
            Merchant merchant = new Merchant();
            BeanUtils.copyProperties(dto, merchant);

            // 🔥 Hutool BCrypt 加密（和Spring Security完全兼容）
            merchant.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));

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
            // 根据明文手机号查询（正确）
            LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Merchant::getPhone, dto.getPhone());
            Merchant merchant = getOne(queryWrapper);

            if (merchant == null) {
                throw new BusinessException(ResultCodeEnum.MERCHANT_NOT_EXIST);
            }

            // 🔥 Hutool BCrypt 密码匹配（和Spring Security完全兼容）
            if (!BCrypt.checkpw(dto.getPassword(), merchant.getPassword())) {
                throw new BusinessException(ResultCodeEnum.MERCHANT_PASSWORD_ERROR);
            }

            // ======================================
            // 🔥 核心修改：只加这一行！登录前先注销旧 token
            // ======================================
            StpLogic merchantStp = SaManager.getStpLogic("merchant");

            // 🔥 先注销旧 token（如果有的话）
            try {
                merchantStp.logout(merchant.getId());
                log.info("[商家登录] 已注销旧 token，商家ID：{}", merchant.getId());
            } catch (Exception e) {
                log.info("[商家登录] 之前未登录，无需注销");
            }

            // 🔥 再登录，生成新 token（你原来的逻辑保持不变）
            merchantStp.login(merchant.getId());
            String token = merchantStp.getTokenValue();

            // 封装返回
            MerchantVO merchantVO = new MerchantVO();
            BeanUtils.copyProperties(merchant, merchantVO);
            merchantVO.setToken(token);

            log.info("[商家登录] 成功，商家ID：{}，新 token：{}", merchant.getId(), token);
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
            MerchantVO cacheVo = (MerchantVO) redisTemplate.opsForValue().get(cacheKey);
            if (cacheVo != null) {
                log.info("[商家-查询信息] 命中Redis缓存");
                return cacheVo;
            }

            Merchant merchant = getById(merchantId);
            if (merchant == null) {
                throw new BusinessException(ResultCodeEnum.MERCHANT_NOT_EXIST);
            }

            MerchantVO vo = new MerchantVO();
            BeanUtils.copyProperties(merchant, vo);

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