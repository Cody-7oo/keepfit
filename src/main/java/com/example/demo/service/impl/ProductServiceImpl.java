package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.dto.ProductAddDTO;
import com.example.demo.common.dto.ProductUpdateDTO;
import com.example.demo.common.enums.ProductStatusEnum;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.vo.ProductVO;
import com.example.demo.entity.Product;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.service.ProductAsyncService;
import com.example.demo.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 🔥 注入你现有的异步服务
    @Resource
    private ProductAsyncService productAsyncService;

    // 缓存 KEY 统一管理
    private static final String PRODUCT_UP_LIST = "product:up:list";
    private static final String PRODUCT_CACHE_PREFIX = "product:category:";
    private static final String PRODUCT_PAGE_CACHE = "product:page:";

    // ====================== 商家新增商品 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(ProductAddDTO dto) {
        log.info("[商品-新增] 入参：{}", dto);
        long start = System.currentTimeMillis();
        try {
            Product product = new Product();
            BeanUtils.copyProperties(dto, product);
            product.setStatus(ProductStatusEnum.ON_SHELF.getCode());
            boolean save = save(product);
            if (!save) {
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
            }

            // 🔥 异步清理所有商品缓存
            productAsyncService.clearProductListCache();
            log.info("[商品-新增] 成功，异步清除缓存");

        } catch (BusinessException e) {
            log.warn("[商品-新增] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[商品-新增] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商品-新增] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 商家修改商品 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductUpdateDTO dto) {
        log.info("[商品-修改] ID：{}", dto.getId());
        long start = System.currentTimeMillis();
        try {
            Product dbProduct = getById(dto.getId());
            if (dbProduct == null) {
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }

            BeanUtils.copyProperties(dto, dbProduct);
            updateById(dbProduct);

            // 🔥 异步清理缓存
            productAsyncService.clearProductCache(dbProduct.getId());
            productAsyncService.clearProductListCache();
            log.info("[商品-修改] 成功，异步清除缓存");

        } catch (BusinessException e) {
            log.warn("[商品-修改] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[商品-修改] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商品-修改] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 查询全部上架商品（带 Redis 缓存）======================
    @Override
    public List<ProductVO> getUpProductList() {
        log.info("[商品-查询上架列表] 从Redis获取");
        long start = System.currentTimeMillis();
        try {
            List<ProductVO> cacheList = (List<ProductVO>) redisTemplate.opsForValue().get(PRODUCT_UP_LIST);
            if (cacheList != null) {
                log.info("[商品-查询上架列表] 命中缓存，数量：{}", cacheList.size());
                return cacheList;
            }

            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode());
            List<Product> list = list(wrapper);

            List<ProductVO> voList = list.stream().map(p -> {
                ProductVO vo = new ProductVO();
                BeanUtils.copyProperties(p, vo);
                return vo;
            }).collect(Collectors.toList());

            redisTemplate.opsForValue().set(PRODUCT_UP_LIST, voList, 30, TimeUnit.MINUTES);
            log.info("[商品-查询上架列表] 未命中缓存，查询数据库，数量：{}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("[商品-查询上架列表] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商品-查询上架列表] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 分类 + 搜索（带 Redis 缓存）======================
    @Override
    public List<ProductVO> getProductList(Integer category, String keyword) {
        log.info("[商品-搜索] 分类：{}，关键词：{}", category, keyword);
        long start = System.currentTimeMillis();
        try {
            String cacheKey = PRODUCT_CACHE_PREFIX + (category == null ? "all" : category)
                    + ":" + (StringUtils.hasText(keyword) ? keyword : "nokeyword");

            List<ProductVO> cacheList = (List<ProductVO>) redisTemplate.opsForValue().get(cacheKey);
            if (cacheList != null) {
                log.info("[商品-搜索] 命中Redis缓存");
                return cacheList;
            }

            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode());

            if (category != null) {
                wrapper.eq(Product::getCategory, category);
            }
            if (StringUtils.hasText(keyword)) {
                wrapper.like(Product::getName, keyword);
            }

            wrapper.orderByDesc(Product::getId);
            List<Product> list = list(wrapper);

            List<ProductVO> voList = list.stream().map(p -> {
                ProductVO vo = new ProductVO();
                BeanUtils.copyProperties(p, vo);
                return vo;
            }).collect(Collectors.toList());

            redisTemplate.opsForValue().set(cacheKey, voList, 20, TimeUnit.MINUTES);
            log.info("[商品-搜索] 查询数据库，数量：{}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("[商品-搜索] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商品-搜索] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 分页查询（🔥 已加 Redis 缓存）======================
    @Override
    public IPage<ProductVO> getProductPage(Integer pageNum, Integer pageSize, Integer category, String keyword) {
        log.info("[商品-分页查询] page：{}，size：{}", pageNum, pageSize);
        long start = System.currentTimeMillis();
        try {
            String cacheKey = PRODUCT_PAGE_CACHE + pageNum + ":" + pageSize
                    + ":" + (category == null ? "all" : category)
                    + ":" + (StringUtils.hasText(keyword) ? keyword : "nokeyword");

            IPage<ProductVO> cachePage = (IPage<ProductVO>) redisTemplate.opsForValue().get(cacheKey);
            if (cachePage != null) {
                log.info("[商品-分页查询] 命中Redis缓存");
                return cachePage;
            }

            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode());

            if (category != null) {
                wrapper.eq(Product::getCategory, category);
            }
            if (StringUtils.hasText(keyword)) {
                wrapper.like(Product::getName, keyword);
            }

            IPage<Product> page = new Page<>(pageNum, pageSize);
            IPage<Product> productPage = page(page, wrapper);

            IPage<ProductVO> voPage = productPage.convert(p -> {
                ProductVO vo = new ProductVO();
                BeanUtils.copyProperties(p, vo);
                return vo;
            });

            redisTemplate.opsForValue().set(cacheKey, voPage, 15, TimeUnit.MINUTES);
            log.info("[商品-分页查询] 查询DB完成，已缓存");
            return voPage;
        } catch (Exception e) {
            log.error("[商品-分页查询] 异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[商品-分页查询] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    // ====================== 删除商品 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        removeById(id);

        // 🔥 异步清理缓存
        productAsyncService.clearProductCache(id);
        productAsyncService.clearProductListCache();
        log.info("[商品-删除成功] 商品ID:{}", id);
    }

    /**
     * 保留原有同步清理方法（内部使用）
     */
    private void clearAllProductCache() {
        try {
            redisTemplate.delete(PRODUCT_UP_LIST);
            redisTemplate.delete(PRODUCT_PAGE_CACHE + "*");
            redisTemplate.delete(PRODUCT_CACHE_PREFIX + "*");
            log.info("[商品缓存] 全部清理完成");
        } catch (Exception e) {
            log.error("[商品缓存] 清理失败", e);
        }
    }
}