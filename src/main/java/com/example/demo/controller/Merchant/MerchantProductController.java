package com.example.demo.controller.Merchant;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.common.util.LogUtil;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.dto.ProductAddDTO;
import com.example.demo.common.dto.ProductUpdateDTO;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.ProductVO;
import com.example.demo.entity.Product;
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
@SaCheckLogin(type = "merchant")
@SaCheckPermission("merchant:product:manage")
public class MerchantProductController {

    @Resource
    private ProductService productService;

    @Resource
    private LogUtil logUtil;

    private Long getLoginMerchantId() {
        return StpUtil.getLoginIdAsLong();
    }

    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid ProductAddDTO dto) {
        Long merchantId = getLoginMerchantId();
        log.info("[商家-新增商品] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            dto.setMerchantId(merchantId);
            productService.addProduct(dto);
            log.info("[业务埋点-商品新增成功] merchantId:{}, productName:{}", merchantId, dto.getProductName());

            logUtil.record("商品管理", "商家新增商品");

            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商品新增失败] merchantId:{}, productName:{}, 失败原因:{}", merchantId, dto.getProductName(), e.getMessage());
            log.error("[商家-新增商品] 异常", e);
            throw e;
        } finally {
            log.info("[商家-新增商品] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @PostMapping("/update")
    public R<Void> update(@RequestBody @Valid ProductUpdateDTO dto) {
        Long merchantId = getLoginMerchantId();
        log.info("[商家-修改商品] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            Product exist = productService.getById(dto.getId());
            if (exist == null) {
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }

            dto.setMerchantId(merchantId);
            productService.updateProduct(dto);
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

    @GetMapping("/list")
    public R<List<ProductVO>> allList() {
        Long merchantId = getLoginMerchantId();
        log.info("[商家-查询商品] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
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

    @PostMapping("/changeStatus")
    public R<Void> changeStatus(
            @RequestParam Long id,
            @RequestParam Integer status
    ) {
        Long merchantId = getLoginMerchantId();
        log.info("[商家-修改状态] 商家ID:{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            Product product = productService.getById(id);
            if (product == null) {
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }

            product.setStatus(status);
            productService.updateById(product);
            log.info("[业务埋点-商品状态变更] merchantId:{}, productId:{}, 目标状态:{}", merchantId, id, status);
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商品状态变更失败] merchantId:{}, productId:{}, 目标状态:{}, 失败原因:{}", merchantId, id, status, e.getMessage());
            log.error("[商家-修改状态] 异常", e);
            throw e;
        } finally {
            log.info("[商家-修改状态] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @PostMapping("/delete")
    public R<Void> delete(@RequestParam Long id) {
        Long merchantId = getLoginMerchantId();
        log.info("[商家-删除商品] 商家ID:{}，商品ID：{}", merchantId, id);
        long start = System.currentTimeMillis();
        try {
            Product product = productService.getById(id);
            if (product == null) {
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }

            productService.deleteProduct(id);
            log.info("[业务埋点-商品删除成功] merchantId:{}, productId:{}", merchantId, id);
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商品删除异常] merchantId:{}, productId:{}, 异常原因:{}", merchantId, id, e.getMessage());
            log.error("[商家-删除商品] 异常：", e);
            throw e;
        } finally {
            log.info("[商家-删除商品] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}