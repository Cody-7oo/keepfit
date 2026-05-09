package com.example.demo.controller.Merchant;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import com.example.demo.annotation.Idempotent;
import com.example.demo.annotation.RateLimit;
import com.example.demo.annotation.RepeatSubmit;
import com.example.demo.common.dto.ProductAddDTO;
import com.example.demo.common.dto.ProductChangeStatusDTO;
import com.example.demo.common.dto.ProductDeleteDTO;
import com.example.demo.common.dto.ProductUpdateDTO;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.ProductVO;
import com.example.demo.entity.Product;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.util.LogUtil;
import com.example.demo.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/merchant/product")
public class MerchantProductController {

    @Resource
    private ProductService productService;

    @Resource
    private LogUtil logUtil;

    /**
     * 获取商家登录逻辑（企业固定写法）
     */
    private StpLogic getMerchantStp() {
        return SaManager.getStpLogic("merchant");
    }

    /**
     * 获取当前登录商家ID
     */
    private Long getLoginMerchantId() {
        return getMerchantStp().getLoginIdAsLong();
    }

    // ==================== 新增商品 ====================
    @Idempotent
    @RepeatSubmit
    @PostMapping("/add")
    @RateLimit(limit = 3, second = 10)
    public R<Void> add(@RequestBody @Valid ProductAddDTO dto) {
        Long merchantId = getLoginMerchantId();
        log.info("[商家-新增商品] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            dto.setMerchantId(merchantId);
            productService.addProduct(dto, merchantId);
            log.info("[业务埋点-商品新增成功] merchantId:{}, productName:{}", merchantId, dto.getName());
            logUtil.record("商品管理", "商家新增商品");
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商品新增失败] merchantId:{}, productName:{}, 失败原因:{}", merchantId, dto.getName(), e.getMessage());
            log.error("[商家-新增商品] 异常", e);
            throw e;
        } finally {
            log.info("[商家-新增商品] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ==================== 修改商品（防越权） ====================
    @Idempotent
    @RepeatSubmit
    @PostMapping("/update")
    @RateLimit(limit = 3, second = 10)
    public R<Void> update(@RequestBody @Valid ProductUpdateDTO dto) {
        Long merchantId = getLoginMerchantId();
        log.info("[商家-修改商品] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            // 🔥 企业标准防越权：查询商品并校验归属
            Product exist = productService.getById(dto.getId());
            if (exist == null) {
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }
            if (!exist.getMerchantId().equals(merchantId)) {
                throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
            }

            dto.setMerchantId(merchantId);
            productService.updateProduct(dto, merchantId);
            log.info("[业务埋点-商品修改成功] merchantId:{}, productId:{}", merchantId, dto.getId());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商品修改异常] merchantId:{}, productId:{}, 异常原因:{}", merchantId, dto.getId(), e.getMessage());
            log.error("[商家-修改商品] 异常", e);
            throw e;
        } finally {
            log.info("[商家-修改商品] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ==================== 查询商品列表（天然防越权） ====================
    @GetMapping("/list")
    @RateLimit(limit = 3, second = 10)
    public R<List<ProductVO>> allList() {
        Long merchantId = getLoginMerchantId();
        log.info("[商家-查询商品] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            // 🔥 企业标准：直接拼接商家ID，只查自己的数据
            List<Product> list = productService.lambdaQuery()
                    .eq(Product::getMerchantId, merchantId)
                    .list();

            List<ProductVO> voList = list.stream().map(p -> {
                ProductVO vo = new ProductVO();
                BeanUtils.copyProperties(p, vo);
                return vo;
            }).collect(Collectors.toList());

            return R.ok(voList);
        } catch (Exception e) {
            log.error("[商家-查询商品] 异常", e);
            throw e;
        } finally {
            log.info("[商家-查询商品] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ==================== 修改商品状态（防越权） ====================
    @Idempotent
    @RepeatSubmit
    @PostMapping("/changeStatus")
    @RateLimit(limit = 3, second = 10)
    public R<Void> changeStatus(@RequestBody @Valid ProductChangeStatusDTO dto) {
        Long merchantId = getLoginMerchantId();
        log.info("[商家-修改状态] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            // 🔥 企业标准防越权
            Product product = productService.getById(dto.getId());
            if (product == null) {
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }
            if (!product.getMerchantId().equals(merchantId)) {
                throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
            }

            product.setStatus(dto.getStatus());
            productService.updateById(product);
            log.info("[业务埋点-商品状态变更] merchantId:{}, productId:{}", merchantId, dto.getId());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商品状态变更失败] merchantId:{}, productId:{}, 失败原因:{}", merchantId, dto.getId(), e.getMessage());
            log.error("[商家-修改状态] 异常", e);
            throw e;
        } finally {
            log.info("[商家-修改状态] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ==================== 删除商品（防越权 + 企业传参规范） ====================
    @Idempotent
    @RepeatSubmit
    @PostMapping("/delete")
    @RateLimit(limit = 3, second = 10)
    public R<Void> delete(@RequestBody @Valid ProductDeleteDTO dto) {
        Long merchantId = getLoginMerchantId();
        Long productId = dto.getId();
        log.info("[商家-删除商品] 商家ID:{}，商品ID：{}", merchantId, productId);
        long start = System.currentTimeMillis();
        try {
            // 🔥 企业标准防越权
            Product product = productService.getById(productId);
            if (product == null) {
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }
            if (!product.getMerchantId().equals(merchantId)) {
                throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
            }

            productService.removeById(productId);
            log.info("[业务埋点-商品删除成功] merchantId:{}, productId:{}", merchantId, productId);
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商品删除异常] merchantId:{}, productId:{}, 异常原因:{}", merchantId, productId, e.getMessage());
            log.error("[商家-删除商品] 异常：", e);
            throw e;
        } finally {
            log.info("[商家-删除商品] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}