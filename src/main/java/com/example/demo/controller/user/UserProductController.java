package com.example.demo.controller.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.RateLimit;
import com.example.demo.common.dto.ProductSearchDTO;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.ProductVO;
import com.example.demo.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    @RateLimit(limit = 3, second = 10)
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

    // ==================== 🔥 修改为 POST + JSON 请求 ====================
    @PostMapping("/search")
    @RateLimit(limit = 3, second = 10)
    public R<List<ProductVO>> search(@RequestBody ProductSearchDTO searchDTO) {
        Long userId = getLoginUserId();
        log.info("[用户-商品搜索] 用户ID：{} | 分类：{} | 关键词：{}",
                userId, searchDTO.getCategory(), searchDTO.getKeyword());
        long start = System.currentTimeMillis();
        try {
            // 直接调用原Service方法，逻辑不变
            return R.ok(productService.getProductList(searchDTO.getCategory(), searchDTO.getKeyword()));
        } catch (Exception e) {
            log.error("[用户-商品搜索] 异常：", e);
            throw e;
        } finally {
            log.info("[用户-商品搜索] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}