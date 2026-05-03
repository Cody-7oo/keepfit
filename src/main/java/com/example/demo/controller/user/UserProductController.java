package com.example.demo.controller.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.ProductVO;
import com.example.demo.service.ProductService;
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
public class UserProductController {

    @Resource
    private ProductService productService;

    private Long getLoginUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @GetMapping("/list")
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
    public R<List<ProductVO>> search(
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) String keyword
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