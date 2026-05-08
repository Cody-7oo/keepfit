package com.example.demo.controller.user;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.dto.OrderCreateDTO;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.OrderVO;
import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.example.demo.common.enums.ResultCodeEnum.ORDER_NO_PERMISSION;

@Slf4j
@RestController
@RequestMapping("/user/order")
@SaCheckLogin(type = "user")
@SaCheckPermission("user:order:create")
public class UserOrderController {

    @Resource
    private OrderService orderService;

    private Long getLoginUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Idempotent
    @RepeatSubmit
    @SaCheckLogin(type = "user") // 登录校验正常
    @PostMapping("/create")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    public R<OrderVO> createOrder(@RequestBody @Valid OrderCreateDTO dto) {
        StpLogic stpLogic = SaManager.getStpLogic("user");
        Long userId = stpLogic.getLoginIdAsLong();

        // ======================
        // 🔥 手动权限校验（100%不报错）
        // ======================
        java.util.List<String> permissions = (java.util.List<String>) stpLogic.getSession().get("permissions");
        if (permissions == null || !permissions.contains("user:order:create")) {
            throw new com.example.demo.exception.BusinessException(ORDER_NO_PERMISSION);
        }

        dto.setUserId(userId);

        log.info("[用户-创建订单] 用户ID：{}", userId);
        long start = System.currentTimeMillis();
        try {
            OrderVO vo = orderService.createOrder(dto);
            log.info("[业务埋点-下单成功] userId:{}, orderNo:{}, totalAmount:{}",
                    userId, vo.getOrderNo(), vo.getTotalPrice());
            return R.ok(vo);
        } catch (Exception e) {
            log.info("[业务埋点-下单失败] userId:{}, 失败原因:{}", userId, e.getMessage());
            log.error("[用户-创建订单] 异常：", e);
            throw e;
        } finally {
            log.info("[用户-创建订单] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @Idempotent
    @RepeatSubmit
    @PostMapping("/cancel")
    @RateLimit(limit = 5, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    public R<Void> cancelOrder(@RequestParam Long orderId) {
        Long userId = getLoginUserId();
        log.info("[用户-取消订单] 用户ID：{}，订单ID：{}", userId, orderId);
        long start = System.currentTimeMillis();

        try {
            Order order = orderService.getById(orderId);
            if (order == null) {
                throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
            }

            orderService.cancelOrder(orderId, userId);
            log.info("[业务埋点-订单取消] userId:{}, orderId:{}, orderNo:{}",
                    userId, orderId, order.getOrderNo());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-订单取消异常] userId:{}, orderId:{}, 异常原因:{}", userId, orderId, e.getMessage());
            log.error("[用户-取消订单] 异常：", e);
            throw e;
        } finally {
            log.info("[用户-取消订单] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @GetMapping("/myList")
    @RateLimit(limit = 10, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    public R<?> getMyOrder() {
        Long userId = getLoginUserId();
        log.info("[用户-查询我的订单] 用户ID：{}", userId);
        long start = System.currentTimeMillis();
        try {
            return R.ok(orderService.getMyOrder(userId));
        } catch (Exception e) {
            log.error("[用户-查询我的订单] 异常：", e);
            throw e;
        } finally {
            log.info("[用户-查询我的订单] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}