package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.dto.CouponUseDTO;
import com.example.demo.common.dto.OrderCreateDTO;
import com.example.demo.common.enums.OrderStatusEnum;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.enums.UserCouponStatusEnum;
import com.example.demo.common.util.SnowflakeIdUtil;
import com.example.demo.common.vo.OrderVO;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.service.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Resource
    private CartService cartService;
    @Resource
    private OrderItemService orderItemService;
    @Resource
    private UserCouponService userCouponService;
    @Resource
    private ProductStockService productStockService;
    @Resource
    private OrderOperationLogService orderOperationLogService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private SnowflakeIdUtil snowflakeIdUtil;

    // 订单缓存 KEY
    private static final String USER_ORDER_KEY = "user:order:list:";

    /**
     * 创建订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO dto) {
        String lockKey = "order:create:lock:" + dto.getUserId();
        RLock lock = redissonClient.getLock(lockKey);

        boolean tryLock;
        try {
            tryLock = lock.tryLock(0, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        }

        if (!tryLock) {
            throw new BusinessException(ResultCodeEnum.REPEAT_SUBMIT);
        }

        try {
            log.info("[订单-创建] 用户ID：{}，入参：{}", dto.getUserId(), dto);
            Long userId = dto.getUserId();
            String receiver = dto.getReceiver();
            String phone = dto.getPhone();
            String address = dto.getAddress();
            Long userCouponId = dto.getUserCouponId();

            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getUserId, userId);
            List<Cart> cartList = cartService.list(wrapper);

            if (cartList == null || cartList.isEmpty()) {
                log.warn("[订单-创建] 购物车为空");
                throw new BusinessException(ResultCodeEnum.CART_EMPTY);
            }

            BigDecimal totalPrice = BigDecimal.ZERO;
            double totalCalorie = 0;
            double totalProtein = 0;
            double totalCarbohydrate = 0;
            double totalFat = 0;

            for (Cart cart : cartList) {
                int num = cart.getNum();
                totalPrice = totalPrice.add(cart.getPrice().multiply(new BigDecimal(num)));
                totalCalorie += cart.getCalorie() * num;
                totalProtein += cart.getProtein() * num;
                totalCarbohydrate += cart.getCarbohydrate() * num;
                totalFat += cart.getFat() * num;
            }

            if (userCouponId != null) {
                log.info("[订单-创建] 使用优惠券：{}", userCouponId);
                CouponUseDTO couponUseDTO = new CouponUseDTO();
                couponUseDTO.setUserCouponId(userCouponId);
                couponUseDTO.setUserId(userId);
                userCouponService.useCoupon(couponUseDTO);
            }

            Order order = new Order();
            order.setOrderNo(String.valueOf(snowflakeIdUtil.nextId()));
            order.setUserId(userId);
            order.setReceiver(receiver);
            order.setPhone(phone);
            order.setAddress(address);
            order.setTotalPrice(totalPrice);
            order.setTotalCalorie(totalCalorie);
            order.setTotalProtein(totalProtein);
            order.setTotalCarbohydrate(totalCarbohydrate);
            order.setTotalFat(totalFat);
            order.setStatus(OrderStatusEnum.UN_PAY.getCode());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            order.setMerchantId(1L);
            order.setUserCouponId(dto.getUserCouponId());

            save(order);

            List<OrderItem> itemList = new ArrayList<>();
            for (Cart cart : cartList) {
                OrderItem item = new OrderItem();
                item.setOrderId(order.getId());
                item.setProductId(cart.getProductId());
                item.setProductName(cart.getProductName());
                item.setPrice(cart.getPrice());
                item.setNum(cart.getNum());
                item.setCalorie(cart.getCalorie());
                item.setProtein(cart.getProtein());
                item.setCarbohydrate(cart.getCarbohydrate());
                item.setFat(cart.getFat());
                itemList.add(item);
            }

            orderItemService.saveBatchItems(itemList);

            for (Cart cart : cartList) {
                productStockService.deductStock(cart.getProductId(), cart.getNum());
            }

            cartService.remove(wrapper);
            userCouponService.grantCoffeeCoupon(userId);

            orderOperationLogService.saveOrderLog(
                    order.getOrderNo(),
                    userId,
                    "CREATE",
                    "用户创建订单",
                    "-",
                    OrderStatusEnum.UN_PAY.getDesc(),
                    null
            );

            // =============== 创建订单后清理用户订单列表缓存 ===============
            redisTemplate.delete(USER_ORDER_KEY + userId);

            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            log.info("[订单-创建] 下单成功，订单号：{}", order.getOrderNo());
            return vo;

        } catch (BusinessException e) {
            log.warn("[订单-创建] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[订单-创建] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 定时任务：超时未支付订单自动取消
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelTimeoutOrder(int timeoutMinutes) {
        RLock lock = redissonClient.getLock("order:timeout:cancel:lock");
        boolean locked;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[定时任务] 获取锁被中断", e);
            return 0;
        }

        if (!locked) {
            log.info("[定时任务] 其他实例正在执行，本次跳过");
            return 0;
        }

        try {
            LocalDateTime expireTime = LocalDateTime.now().minusMinutes(timeoutMinutes);
            QueryWrapper<Order> wrapper = new QueryWrapper<>();
            wrapper.eq("status", OrderStatusEnum.UN_PAY.getCode());
            wrapper.lt("create_time", expireTime);
            wrapper.eq("is_deleted", 0);

            List<Order> orderList = list(wrapper);
            int count = 0;

            for (Order order : orderList) {
                Long orderId = order.getId();
                Long userId = order.getUserId();

                if (order.getUserCouponId() != null) {
                    UserCoupon userCoupon = userCouponService.getById(order.getUserCouponId());
                    if (userCoupon != null) {
                        userCoupon.setStatus(UserCouponStatusEnum.UN_USED.getCode());
                        userCoupon.setUseTime(null);
                        userCouponService.updateById(userCoupon);
                        redisTemplate.delete("user:coupon:" + userId);
                    }
                }

                List<OrderItem> itemList = orderItemService.lambdaQuery()
                        .eq(OrderItem::getOrderId, orderId)
                        .list();

                for (OrderItem item : itemList) {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    cart.setProductId(item.getProductId());
                    cart.setProductName(item.getProductName());
                    cart.setPrice(item.getPrice());
                    cart.setNum(item.getNum());
                    cart.setCalorie(item.getCalorie());
                    cart.setProtein(item.getProtein());
                    cart.setCarbohydrate(item.getCarbohydrate());
                    cart.setFat(item.getFat());
                    cartService.save(cart);
                }

                for (OrderItem item : itemList) {
                    productStockService.rollbackStock(item.getProductId(), item.getNum());
                }

                order.setStatus(OrderStatusEnum.CANCEL.getCode());
                order.setUpdateTime(LocalDateTime.now());
                updateById(order);

                orderOperationLogService.saveOrderLog(
                        order.getOrderNo(),
                        userId,
                        "TIMEOUT_CANCEL",
                        "订单超时未支付，系统自动取消",
                        OrderStatusEnum.UN_PAY.getDesc(),
                        OrderStatusEnum.CANCEL.getDesc(),
                        null
                );

                // 超时取消 → 清理订单缓存
                redisTemplate.delete(USER_ORDER_KEY + userId);
                count++;
            }

            log.info("[定时任务] 成功取消超时订单：{} 单", count);
            return count;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 用户取消订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        String lockKey = "order:cancel:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                throw new BusinessException(ResultCodeEnum.REPEAT_SUBMIT);
            }

            log.info("[订单-取消] 订单ID：{}，用户ID：{}", orderId, userId);
            Order order = getById(orderId);

            if (order == null) {
                throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
            }
            if (!order.getUserId().equals(userId)) {
                throw new BusinessException(ResultCodeEnum.ORDER_NO_PERMISSION);
            }
            if (order.getStatus() == OrderStatusEnum.COMPLETED.getCode() ||
                    order.getStatus() == OrderStatusEnum.CANCEL.getCode()) {
                throw new BusinessException(ResultCodeEnum.ORDER_STATUS_NOT_ALLOW);
            }

            if (order.getUserCouponId() != null) {
                UserCoupon userCoupon = userCouponService.getById(order.getUserCouponId());
                if (userCoupon != null) {
                    userCoupon.setStatus(UserCouponStatusEnum.UN_USED.getCode());
                    userCoupon.setUseTime(null);
                    userCouponService.updateById(userCoupon);
                    redisTemplate.delete("user:coupon:" + userId);
                    log.info("[订单-取消] 优惠券已退还");
                }
            }

            List<OrderItem> itemList = orderItemService.lambdaQuery()
                    .eq(OrderItem::getOrderId, orderId)
                    .list();

            for (OrderItem item : itemList) {
                Cart cart = new Cart();
                cart.setUserId(userId);
                cart.setProductId(item.getProductId());
                cart.setProductName(item.getProductName());
                cart.setPrice(item.getPrice());
                cart.setNum(item.getNum());
                cart.setCalorie(item.getCalorie());
                cart.setProtein(item.getProtein());
                cart.setCarbohydrate(item.getCarbohydrate());
                cart.setFat(item.getFat());
                cartService.save(cart);
            }
            log.info("[订单-取消] 购物车已恢复");

            for (OrderItem item : itemList) {
                productStockService.rollbackStock(item.getProductId(), item.getNum());
            }

            order.setStatus(OrderStatusEnum.CANCEL.getCode());
            order.setUpdateTime(LocalDateTime.now());
            updateById(order);

            orderOperationLogService.saveOrderLog(
                    order.getOrderNo(),
                    userId,
                    "USER_CANCEL",
                    "用户主动取消订单",
                    OrderStatusEnum.UN_PAY.getDesc(),
                    OrderStatusEnum.CANCEL.getDesc(),
                    null
            );

            // =============== 取消订单 → 清理用户订单缓存 ===============
            redisTemplate.delete(USER_ORDER_KEY + userId);

            log.info("[订单-取消] 取消成功");

        } catch (BusinessException e) {
            log.warn("[订单-取消] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[订单-取消] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询我的订单（加 Redis 缓存）
     */
    @Override
    public List<OrderVO> getMyOrder(Long userId) {
        log.info("[订单-我的订单] 用户ID：{}", userId);
        try {
            String cacheKey = USER_ORDER_KEY + userId;

            // 先读缓存
            List<OrderVO> cacheList = (List<OrderVO>) redisTemplate.opsForValue().get(cacheKey);
            if (cacheList != null) {
                log.info("[订单-我的订单] 命中Redis缓存");
                return cacheList;
            }

            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getUserId, userId);
            wrapper.orderByDesc(Order::getCreateTime);
            List<Order> orderList = list(wrapper);

            List<OrderVO> voList = orderList.stream().map(order -> {
                OrderVO vo = new OrderVO();
                BeanUtils.copyProperties(order, vo);
                return vo;
            }).collect(Collectors.toList());

            // 写入缓存 5分钟
            redisTemplate.opsForValue().set(cacheKey, voList, 5, TimeUnit.MINUTES);
            log.info("[订单-我的订单] 查询DB，缓存已更新");
            return voList;

        } catch (Exception e) {
            log.error("[订单-我的订单] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 商家修改订单状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantChangeStatus(Long orderId, Integer newStatus, Long merchantId) {
        String lockKey = "order:merchant:change:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                throw new BusinessException(ResultCodeEnum.REPEAT_SUBMIT);
            }

            log.info("[订单-商家改状态] 商家ID:{}，订单ID：{}，目标状态：{}", merchantId, orderId, newStatus);
            Order order = getById(orderId);

            if (order == null) {
                throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
            }

            if (!order.getMerchantId().equals(merchantId)) {
                throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
            }

            Integer now = order.getStatus();
            String oldStatusDesc = OrderStatusEnum.getDescByCode(now);
            String newStatusDesc = OrderStatusEnum.getDescByCode(newStatus);

            if (now == OrderStatusEnum.COMPLETED.getCode() ||
                    now == OrderStatusEnum.CANCEL.getCode()) {
                throw new BusinessException(ResultCodeEnum.ORDER_STATUS_NOT_ALLOW);
            }

            boolean allow = false;
            if (now == OrderStatusEnum.UN_PAY.getCode() && newStatus == OrderStatusEnum.PREPARE.getCode()) {
                allow = true;
            }
            if (now == OrderStatusEnum.PREPARE.getCode() && newStatus == OrderStatusEnum.DELIVERING.getCode()) {
                allow = true;
            }
            if (now == OrderStatusEnum.DELIVERING.getCode() && newStatus == OrderStatusEnum.COMPLETED.getCode()) {
                allow = true;
                order.setCompleteTime(LocalDateTime.now());
            }

            if (!allow) {
                throw new BusinessException(ResultCodeEnum.ORDER_STATUS_NOT_ALLOW);
            }

            order.setStatus(newStatus);
            order.setUpdateTime(LocalDateTime.now());
            updateById(order);

            orderOperationLogService.saveOrderLog(
                    order.getOrderNo(),
                    merchantId,
                    "MERCHANT_CHANGE_STATUS",
                    "商家修改订单状态",
                    oldStatusDesc,
                    newStatusDesc,
                    null
            );

            // =============== 商家改状态 → 清理用户订单缓存 ===============
            redisTemplate.delete(USER_ORDER_KEY + order.getUserId());

            log.info("[订单-商家改状态] 成功");

        } catch (BusinessException e) {
            log.warn("[订单-商家改状态] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[订单-商家改状态] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 商家查询订单
     */
    @Override
    public List<OrderVO> merchantOrderList(Long merchantId) {
        log.info("[商家-查询订单] 商家ID：{}", merchantId);
        try {
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getMerchantId, merchantId);
            wrapper.orderByDesc(Order::getCreateTime);

            List<Order> orderList = list(wrapper);
            return orderList.stream().map(order -> {
                OrderVO vo = new OrderVO();
                BeanUtils.copyProperties(order, vo);
                return vo;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[商家-查询订单] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }
}