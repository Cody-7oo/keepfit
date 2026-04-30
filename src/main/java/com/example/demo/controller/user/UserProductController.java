package com.example.demo.controller.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.AntiReplay;
import com.example.demo.annotation.ApiSignature;
import com.example.demo.annotation.DataScope;
import com.example.demo.annotation.RateLimit;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.ProductVO;
import com.example.demo.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/product")
@SaCheckLogin(type = "user")
@Api(tags = "用户 - 商品查询")
public class UserProductController {

    @Resource
    private ProductService productService;

    private Long getLoginUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @GetMapping("/list")
    @ApiOperation("查询所有已上架商品列表")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    public R<List<ProductVO>> list() {
        Long userId = getLoginUserId();
        log.info("[用户-查询上架商品列表] 用户ID：{}", userId);
        long start = System.currentTimeMillis();
        try {
            return R.ok(productService.getUpProductList());
        } catch (Exception e) {
            log.error("[用户-查询上架商品列表] 异常：", e);
            throw e;
        } finally {
            log.info("[用户-查询上架商品列表] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @GetMapping("/search")
    @ApiOperation("商品分类筛选 + 关键词搜索")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    public R<List<ProductVO>> search(
            @ApiParam(value = "商品分类ID", required = false) @RequestParam(required = false) Integer category,
            @ApiParam(value = "搜索关键词", required = false) @RequestParam(required = false) String keyword
    ) {
        Long userId = getLoginUserId();
        log.info("[用户-商品搜索] 用户ID：{} | 分类：{} | 关键词：{}", userId, category, keyword);
        long start = System.currentTimeMillis();
        try {
            return R.ok(productService.getProductList(category, keyword));
        } catch (Exception e) {
            log.error("[用户-商品搜索] 异常：", e);
            throw e;
        } finally {
            log.info("[用户-商品搜索] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}