package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    // ====================== 【企业版】加入购物车：DB+Redis双写 ======================
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
            log.info("[购物车-排查] 前端传入productId={}, 查询到的商品ID={}, 商品名={}",
                    dto.getProductId(), product.getId(), product.getName());

            // 1. 数据库操作：查询是否已存在
            Cart cart = this.lambdaQuery()
                    .eq(Cart::getUserId, dto.getUserId())
                    .eq(Cart::getProductId, dto.getProductId())
                    .one();

            if (cart != null) {
                // 存在：叠加数量
                cart.setNum(cart.getNum() + dto.getNum());
                this.updateById(cart);
                log.info("[购物车-加入] 数据库更新数量：productId={}", dto.getProductId());
            } else {
                // 不存在：新增购物车
                cart = new Cart();
                cart.setUserId(dto.getUserId());
                cart.setProductId(product.getId());
                cart.setNum(dto.getNum());
                cart.setPrice(product.getPrice());
                this.save(cart);
                log.info("[购物车-加入] 数据库新增商品：productId={}", dto.getProductId());
            }

            // 2. Redis操作（原有逻辑不变）
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
                exist.put("num", (Integer) exist.get("num") + dto.getNum());
                hashOps.put(key, field, exist);
            } else {
                cartItem.put("num", dto.getNum());
                hashOps.put(key, field, cartItem);
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

    // ====================== 【企业版】+/-修改数量：DB+Redis双写 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNum(CartUpdateDTO dto) {
        log.info("[购物车-修改数量] 入参：{}", dto);
        long start = System.currentTimeMillis();
        try {
            if (dto.getUserId() == null || dto.getProductId() == null || dto.getNum() == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
            }
            String key = CART_PREFIX + dto.getUserId();
            String field = "p:" + dto.getProductId();
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

            // 商品不存在：仅+1可新增
            if (!hashOps.hasKey(key, field)) {
                if (dto.getNum() == 1) {
                    Product product = productService.getById(dto.getProductId());
                    if (product == null) throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);

                    // 数据库新增
                    Cart cart = new Cart();
                    cart.setUserId(dto.getUserId());
                    cart.setProductId(product.getId());
                    cart.setNum(1);
                    cart.setPrice(product.getPrice());
                    this.save(cart);

                    // Redis新增
                    Map<String, Object> cartItem = new HashMap<>();
                    cartItem.put("productId", product.getId());
                    cartItem.put("productName", product.getName());
                    cartItem.put("price", product.getPrice());
                    cartItem.put("calorie", product.getCalorie());
                    cartItem.put("protein", product.getProtein());
                    cartItem.put("carbohydrate", product.getCarbohydrate());
                    cartItem.put("fat", product.getFat());
                    cartItem.put("num", 1);
                    hashOps.put(key, field, cartItem);
                    redisTemplate.expire(key, CART_EXPIRE, TimeUnit.DAYS);
                    log.info("[购物车-修改数量] 自动新增：productId={}", dto.getProductId());
                } else {
                    throw new BusinessException(ResultCodeEnum.CART_NOT_EXIST);
                }
                return;
            }

            // 获取当前数量
            Map<String, Object> cartItem = (Map<String, Object>) hashOps.get(key, field);
            int oldNum = (Integer) cartItem.get("num");
            int newNum = oldNum + dto.getNum();

            // 数据库操作
            Cart dbCart = this.lambdaQuery()
                    .eq(Cart::getUserId, dto.getUserId())
                    .eq(Cart::getProductId, dto.getProductId())
                    .one();
            if (dbCart == null) throw new BusinessException(ResultCodeEnum.CART_NOT_EXIST);

            // 数量≤0：删除商品
            if (newNum <= 0) {
                this.removeById(dbCart.getId()); // 删库
                hashOps.delete(key, field); // 删Redis
                log.info("[购物车-修改数量] 商品已删除：productId={}", dto.getProductId());
            } else {
                // 更新数量
                dbCart.setNum(newNum);
                this.updateById(dbCart); // 更新库
                cartItem.put("num", newNum);
                hashOps.put(key, field, cartItem); // 更新Redis
                log.info("[购物车-修改数量] 更新数量：旧={},新={}", oldNum, newNum);
            }

        } catch (BusinessException e) {
            log.warn("[购物车-修改数量] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[购物车-修改数量] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[购物车-修改数量] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 【企业版】删除商品：DB+Redis双删 ======================
    @Override
    public void deleteProduct(Long userId, Long productId) {
        log.info("[购物车-删除商品] userId={}, productId={}", userId, productId);
        long start = System.currentTimeMillis();
        try {
            // 🔥 【修复点1】先查询购物车中是否存在该商品
            Cart existCart = this.lambdaQuery()
                    .eq(Cart::getUserId, userId)
                    .eq(Cart::getProductId, productId)
                    .one();

            // 🔥 【修复点2】不存在 → 直接抛业务异常（前端会收到错误提示）
            if (existCart == null) {
                log.warn("[购物车-删除商品] 商品不存在，无法删除：userId={}, productId={}", userId, productId);
                throw new BusinessException(ResultCodeEnum.CART_NOT_EXIST);
            }

            // 🔥 【修复点3】存在 → 执行删除（DB+Redis）
            // 删数据库
            this.remove(new LambdaQueryWrapper<Cart>()
                    .eq(Cart::getUserId, userId)
                    .eq(Cart::getProductId, productId));
            // 删Redis
            String key = CART_PREFIX + userId;
            String field = "p:" + productId;
            redisTemplate.opsForHash().delete(key, field);

            log.info("[购物车-删除商品] 成功");
        } catch (BusinessException e) {
            // 业务异常直接抛出，不打印冗余日志
            throw e;
        } catch (Exception e) {
            log.error("[购物车-删除商品] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[购物车-删除商品] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 【企业版】查询购物车：Redis优先，DB兜底 ======================
    @Override
    public Map<String, Object> myCart(Long userId) {
        log.info("[购物车-我的购物车] userId={}", userId);
        long start = System.currentTimeMillis();
        try {
            String key = CART_PREFIX + userId;
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            Map<String, Object> entries = hashOps.entries(key);

            // Redis无数据：从数据库查询，回写Redis
            if (entries.isEmpty()) {
                List<Cart> cartList = this.lambdaQuery().eq(Cart::getUserId, userId).list();
                for (Cart cart : cartList) {
                    Product product = productService.getById(cart.getProductId());
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", product.getId());
                    item.put("productName", product.getName());
                    item.put("price", product.getPrice());
                    item.put("calorie", product.getCalorie());
                    item.put("protein", product.getProtein());
                    item.put("carbohydrate", product.getCarbohydrate());
                    item.put("fat", product.getFat());
                    item.put("num", cart.getNum());
                    hashOps.put(key, "p:" + product.getId(), item);
                }
                redisTemplate.expire(key, CART_EXPIRE, TimeUnit.DAYS);
                entries = hashOps.entries(key);
            }

            // 封装返回（修复类型转换错误）
            List<CartVO> cartVOList = new ArrayList<>();
            BigDecimal totalPrice = BigDecimal.ZERO;
            double totalCalorie = 0, totalProtein = 0, totalCarb = 0, totalFat = 0;
            for (Object obj : entries.values()) {
                Map<String, Object> item = (Map<String, Object>) obj;
                CartVO vo = new CartVO();
                // 🔥 修复这里：Long类型直接强转，不再转Integer
                vo.setProductId((Long) item.get("productId"));
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

    // 下单清空购物车
    @Override
    public void clearUserCart(Long userId) {
        log.info("[购物车-清空] userId={}", userId);
        try {
            // 清空DB
            this.remove(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
            // 清空Redis
            redisTemplate.delete(CART_PREFIX + userId);
        } catch (Exception e) {
            log.error("[购物车-清空] 异常：", e);
        }
    }

    // 定时清理过期购物车
    @Scheduled(cron = "0 0 2 * * ?")
    public void clearExpiredCartCache() {
        log.info("[定时任务] 开始清理已过期的购物车Redis缓存");
        try {
            Set<String> keys = redisTemplate.keys(CART_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[定时任务] 清理过期购物车缓存完成");
            }
        } catch (Exception e) {
            log.error("[定时任务] 清理购物车异常", e);
        }
    }
}