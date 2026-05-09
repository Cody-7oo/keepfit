package com.example.demo.controller.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.RateLimit;
import com.example.demo.annotation.RepeatSubmit;
import com.example.demo.common.dto.CartAddDTO;
import com.example.demo.common.dto.CartDeleteDTO;
import com.example.demo.common.dto.CartUpdateDTO;
import com.example.demo.common.result.R;
import com.example.demo.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user/cart")
@SaCheckLogin(type = "user")
public class CartController {

    @Resource
    private CartService cartService;

    private Long getLoginUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 加入购物车
     * JSON参数：{"productId":11,"num":1}
     */
    @RepeatSubmit
    @PostMapping("/add")
    @RateLimit(limit = 3, second = 10)
    public R<Void> add(@RequestBody @Valid CartAddDTO dto) {
        Long userId = getLoginUserId();
        dto.setUserId(userId);

        log.info("[用户-加入购物车] 用户ID：{}，商品ID：{}", userId, dto.getProductId());
        long start = System.currentTimeMillis();
        try {
            cartService.addCart(dto);
            log.info("[业务埋点-加入购物车] userId:{}, productId:{}, 数量:{}", userId, dto.getProductId(), dto.getNum());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-加入购物车失败] userId:{}, productId:{}, 失败原因:{}", userId, dto.getProductId(), e.getMessage());
            log.error("[用户-加入购物车] 异常", e);
            throw e;
        } finally {
            log.info("[用户-加入购物车] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 修改购物车数量（+/-）
     * JSON参数：{"productId":11,"num":1} 或 {"productId":11,"num":-1}
     */
    @RepeatSubmit
    @PostMapping("/update")
    @RateLimit(limit = 3, second = 10)
    public R<Void> update(@RequestBody @Valid CartUpdateDTO dto) {
        Long userId = getLoginUserId();
        dto.setUserId(userId);

        log.info("[用户-修改购物车] 用户ID：{}，商品ID：{}", userId, dto.getProductId());
        long start = System.currentTimeMillis();

        try {
            cartService.updateNum(dto);
            log.info("[业务埋点-购物车变更] userId:{}, productId:{}, 操作类型:修改数量, 新数量:{}",
                    userId, dto.getProductId(), dto.getNum());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-购物车变更异常] userId:{}, productId:{}, 异常原因:{}", userId, dto.getProductId(), e.getMessage());
            log.error("[用户-修改购物车] 异常", e);
            throw e;
        } finally {
            log.info("[用户-修改购物车] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 删除购物车商品
     * JSON参数：{"productId":11}
     */
    @RepeatSubmit
    @PostMapping("/delete")
    @RateLimit(limit = 3, second = 10)
    public R<Void> delete(@RequestBody @Valid CartDeleteDTO dto) {
        Long userId = getLoginUserId();
        Long productId = dto.getProductId();

        log.info("[用户-删除购物车] 用户ID：{}，商品ID：{}", userId, productId);
        long start = System.currentTimeMillis();

        try {
            cartService.deleteProduct(userId, productId);
            log.info("[业务埋点-购物车变更] userId:{}, productId:{}, 操作类型:删除", userId, productId);
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-购物车删除异常] userId:{}, productId:{}, 异常原因:{}", userId, productId, e.getMessage());
            log.error("[用户-删除购物车] 异常", e);
            throw e;
        } finally {
            log.info("[用户-删除购物车] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 获取我的购物车（GET请求，无请求体）
     */
    @GetMapping("/myCart")
    @RateLimit(limit = 3, second = 10)
    public R<Map<String, Object>> myCart() {
        Long userId = getLoginUserId();
        log.info("[用户-我的购物车] 用户ID：{}", userId);
        long start = System.currentTimeMillis();

        try {
            return R.ok(cartService.myCart(userId));
        } catch (Exception e) {
            log.error("[用户-我的购物车] 异常", e);
            throw e;
        } finally {
            log.info("[用户-我的购物车] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}