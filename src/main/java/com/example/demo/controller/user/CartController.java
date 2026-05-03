package com.example.demo.controller.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.dto.CartAddDTO;
import com.example.demo.common.dto.CartUpdateDTO;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.result.R;
import com.example.demo.entity.Cart;
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
@SaCheckPermission("merchant:product:manage")
public class CartController {

    @Resource
    private CartService cartService;

    private Long getLoginUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @RepeatSubmit
    @PostMapping("/add")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
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

    @RepeatSubmit
    @PostMapping("/update")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    public R<Void> update(@RequestBody @Valid CartUpdateDTO dto) {
        Long userId = getLoginUserId();
        log.info("[用户-修改购物车] 用户ID：{}", userId);
        long start = System.currentTimeMillis();

        try {
            Cart cart = cartService.getById(dto.getId());
            if (cart == null) {
                throw new BusinessException(ResultCodeEnum.CART_NOT_EXIST);
            }

            cartService.updateNum(dto);
            log.info("[业务埋点-购物车变更] userId:{}, productId:{}, 操作类型:修改数量, 新数量:{}",
                    userId, cart.getProductId(), dto.getNum());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-购物车变更异常] userId:{}, cartId:{}, 异常原因:{}", userId, dto.getId(), e.getMessage());
            log.error("[用户-修改购物车] 异常", e);
            throw e;
        } finally {
            log.info("[用户-修改购物车] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @RepeatSubmit
    @PostMapping("/delete")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    public R<Void> delete(@RequestParam Long id) {
        Long userId = getLoginUserId();
        log.info("[用户-删除购物车] 用户ID：{}，购物车ID：{}", userId, id);
        long start = System.currentTimeMillis();

        try {
            Cart cart = cartService.getById(id);
            if (cart == null) {
                throw new BusinessException(ResultCodeEnum.CART_NOT_EXIST);
            }

            cartService.delete(id);
            log.info("[业务埋点-购物车变更] userId:{}, productId:{}, 操作类型:删除", userId, cart.getProductId());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-购物车删除异常] userId:{}, cartId:{}, 异常原因:{}", userId, id, e.getMessage());
            log.error("[用户-删除购物车] 异常", e);
            throw e;
        } finally {
            log.info("[用户-删除购物车] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @GetMapping("/myCart")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
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