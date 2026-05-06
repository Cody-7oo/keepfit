package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.dto.CartAddDTO;
import com.example.demo.common.dto.CartUpdateDTO;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.vo.CartVO;
import com.example.demo.entity.Cart;
import com.example.demo.entity.Product;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.CartMapper;
import com.example.demo.service.CartService;
import com.example.demo.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Resource
    private ProductService productService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CART_PREFIX = "user:cart:";
    private static final long CART_EXPIRE = 30;

    // ====================== 加入购物车（Redis） ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCart(CartAddDTO dto) {
        log.info("[购物车-加入] 入参：userId={}, productId={}, num={}",
                dto.getUserId(), dto.getProductId(), dto.getNum());
        long start = System.currentTimeMillis();
        try {
            Product product = productService.getById(dto.getProductId());
            if (product == null) {
                log.warn("[购物车-加入] 商品不存在：productId={}", dto.getProductId());
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }

            String key = CART_PREFIX + dto.getUserId();
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            String field = "p:" + dto.getProductId();

            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("productId", product.getId());
            cartItem.put("productName", product.getName());
            cartItem.put("price", product.getPrice());
            cartItem.put("calorie", product.getCalorie());
            cartItem.put("protein", product.getProtein());
            cartItem.put("carbohydrate", product.getCarbohydrate());
            cartItem.put("fat", product.getFat());

            if (hashOps.hasKey(key, field)) {
                Map<String, Object> exist = (Map<String, Object>) hashOps.get(key, field);
                Integer oldNum = (Integer) exist.get("num");
                exist.put("num", oldNum + dto.getNum());
                hashOps.put(key, field, exist);
                log.info("[购物车-加入] 商品数量叠加：productId={}", dto.getProductId());
            } else {
                cartItem.put("num", dto.getNum());
                hashOps.put(key, field, cartItem);
                log.info("[购物车-加入] 新增商品到购物车：productId={}", dto.getProductId());
            }

            redisTemplate.expire(key, CART_EXPIRE, TimeUnit.DAYS);
        } catch (BusinessException e) {
            log.warn("[购物车-加入] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[购物车-加入] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[购物车-加入] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 修改数量（Redis） ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNum(CartUpdateDTO dto) {
        log.info("[购物车-修改数量] 入参：{}", dto);
        long start = System.currentTimeMillis();
        try {
            log.warn("[购物车-修改数量] 暂基于Redis+productId实现，需前端传入productId");
        } catch (Exception e) {
            log.error("[购物车-修改数量] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[购物车-修改数量] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 删除（Redis） ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("[购物车-删除] ID：{}", id);
        long start = System.currentTimeMillis();
        try {
            log.warn("[购物车-删除] Redis模式不使用自增ID，建议调用deleteProduct接口");
        } catch (Exception e) {
            log.error("[购物车-删除] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[购物车-删除] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 删除指定商品（推荐使用） ======================
    public void deleteProduct(Long userId, Long productId) {
        log.info("[购物车-删除商品] userId={}, productId={}", userId, productId);
        long start = System.currentTimeMillis();
        try {
            String key = CART_PREFIX + userId;
            String field = "p:" + productId;
            redisTemplate.opsForHash().delete(key, field);
            log.info("[购物车-删除商品] 成功");
        } catch (Exception e) {
            log.error("[购物车-删除商品] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[购物车-删除商品] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 我的购物车（完整VO + 营养统计）【Redis 版本】 ======================
    @Override
    public Map<String, Object> myCart(Long userId) {
        log.info("[购物车-我的购物车] userId={}", userId);
        long start = System.currentTimeMillis();
        try {
            String key = CART_PREFIX + userId;
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            Map<String, Object> entries = hashOps.entries(key);

            List<CartVO> cartVOList = new ArrayList<>();
            BigDecimal totalPrice = BigDecimal.ZERO;
            double totalCalorie = 0;
            double totalProtein = 0;
            double totalCarb = 0;
            double totalFat = 0;

            for (Object obj : entries.values()) {
                Map<String, Object> item = (Map<String, Object>) obj;

                CartVO vo = new CartVO();
                vo.setProductId(((Integer) item.get("productId")).longValue());
                vo.setProductName((String) item.get("productName"));
                vo.setPrice((BigDecimal) item.get("price"));
                vo.setNum((Integer) item.get("num"));
                vo.setCalorie((Double) item.get("calorie"));
                vo.setProtein((Double) item.get("protein"));
                vo.setCarbohydrate((Double) item.get("carbohydrate"));
                vo.setFat((Double) item.get("fat"));

                cartVOList.add(vo);

                int num = vo.getNum();
                totalPrice = totalPrice.add(vo.getPrice().multiply(new BigDecimal(num)));
                totalCalorie += vo.getCalorie() * num;
                totalProtein += vo.getProtein() * num;
                totalCarb += vo.getCarbohydrate() * num;
                totalFat += vo.getFat() * num;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("cartList", cartVOList);
            data.put("totalPrice", totalPrice);
            data.put("totalCalorie", totalCalorie);
            data.put("totalProtein", totalProtein);
            data.put("totalCarbohydrate", totalCarb);
            data.put("totalFat", totalFat);

            log.info("[购物车-我的购物车] 商品数量：{}", cartVOList.size());
            return data;
        } catch (Exception e) {
            log.error("[购物车-我的购物车] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[购物车-我的购物车] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 下单后清空购物车 ======================
    public void clearUserCart(Long userId) {
        log.info("[购物车-清空] userId={}", userId);
        try {
            redisTemplate.delete(CART_PREFIX + userId);
        } catch (Exception e) {
            log.error("[购物车-清空] 异常：", e);
        }
    }

    // ====================== 🔥 定时清理购物车Redis缓存（去掉分布式锁） ======================
    @Scheduled(cron = "0 0 2 * * ?")
    public void clearExpiredCartCache() {
        log.info("[定时任务] 开始清理已过期的购物车Redis缓存");
        try {
            Set<String> keys = redisTemplate.keys(CART_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[定时任务] 清理过期购物车缓存完成，共清理：{} 个用户", keys.size());
            }
        } catch (Exception e) {
            log.error("[定时任务] 清理购物车异常", e);
        }
    }
}