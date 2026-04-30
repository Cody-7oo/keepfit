package com.example.demo.controller.Merchant;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.*;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.OrderVO;
import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/merchant/order")
@SaCheckLogin(type = "merchant")
@SaCheckPermission("merchant:order:op")
@Api(tags = "商家 - 订单管理")
public class MerchantOrderController {

    @Resource
    private OrderService orderService;

    @Idempotent
    @RepeatSubmit
    @PostMapping("/changeStatus")
    @ApiOperation("商家修改订单状态（发货/完成等）")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "merchant")
    @ApiSignature
    @AntiReplay
    public R<Void> changeStatus(
            @ApiParam(value = "订单ID", required = true) @RequestParam Long orderId,
            @ApiParam(value = "订单状态", required = true) @RequestParam Integer status
    ) {
        Long merchantId = StpUtil.getLoginIdAsLong();
        log.info("[商家-修改订单状态] 商家ID:{}, orderId:{}, status:{}", merchantId, orderId, status);
        long start = System.currentTimeMillis();
        try {
            Order order = orderService.getById(orderId);
            orderService.merchantChangeStatus(orderId, status, merchantId);

            log.info("[业务埋点-商家发货/订单状态变更] merchantId:{}, orderId:{}, orderNo:{}, 目标状态:{}",
                    merchantId, orderId, order != null ? order.getOrderNo() : "未知", status);
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商家修改订单状态异常] merchantId:{}, orderId:{}, 目标状态:{}, 异常原因:{}",
                    merchantId, orderId, status, e.getMessage());
            log.error("[商家-修改订单状态] 异常：", e);
            throw e;
        } finally {
            log.info("[商家-修改订单状态] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @GetMapping("/list")
    @ApiOperation("商家查询自己店铺的订单列表")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "merchant")
    @ApiSignature
    @AntiReplay
    public R<List<OrderVO>> list() {
        Long merchantId = StpUtil.getLoginIdAsLong();
        log.info("[商家-查询订单] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            return R.ok(orderService.merchantOrderList(merchantId));
        } catch (Exception e) {
            log.error("[商家-查询订单] 异常：", e);
            throw e;
        } finally {
            log.info("[商家-查询订单] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}