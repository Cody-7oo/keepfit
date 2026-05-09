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

    @Resource
    private ProductAsyncService productAsyncService;

    private static final String PRODUCT_UP_LIST = "product:up:list";
    private static final String PRODUCT_CACHE_PREFIX = "product:category:";
    private static final String PRODUCT_PAGE_CACHE = "product:page:";

    // ====================== 商家新增商品（增加越权校验）======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(ProductAddDTO dto, Long merchantId) { // 🔥 增加参数
        log.info("[商品-新增] 入参：{}", dto);
        long start = System.currentTimeMillis();
        try {
            Product product = new Product();
            BeanUtils.copyProperties(dto, product);
            product.setMerchantId(merchantId); // 🔥 强制设置
            product.setStatus(ProductStatusEnum.ON_SHELF.getCode());

            // ======================================
            // 🔥 给非必填字段设置默认值
            // ======================================
            if (product.getStock() == null) {
                product.setStock(0); // 库存默认0
            }

            boolean save = save(product);
            if (!save) {
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
            }

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

    // ====================== 商家修改商品（增加越权校验）======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductUpdateDTO dto, Long merchantId) { // 🔥 增加参数
        log.info("[商品-修改] ID：{}", dto.getId());
        long start = System.currentTimeMillis();
        try {
            Product dbProduct = getById(dto.getId());
            if (dbProduct == null) {
                throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
            }

            // ======================================
            // 🔥 核心：越权校验！确保这个商品属于当前商家
            // ======================================
            if (!dbProduct.getMerchantId().equals(merchantId)) {
                log.warn("[商品-修改] 越权操作！当前商家ID：{}，商品归属商家ID：{}", merchantId, dbProduct.getMerchantId());
                throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
            }

            BeanUtils.copyProperties(dto, dbProduct);
            dbProduct.setMerchantId(merchantId); // 🔥 强制设置
            updateById(dbProduct);

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

    // ====================== 查询全部上架商品（保持不变）======================
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

    // ====================== 分类 + 搜索（保持不变）======================
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

    // ====================== 分页查询（保持不变）======================
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

    // ====================== 删除商品（增加越权校验）======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id, Long merchantId) { // 🔥 增加参数
        Product product = getById(id);
        if (product == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }

        // ======================================
        // 🔥 核心：越权校验！确保这个商品属于当前商家
        // ======================================
        if (!product.getMerchantId().equals(merchantId)) {
            log.warn("[商品-删除] 越权操作！当前商家ID：{}，商品归属商家ID：{}", merchantId, product.getMerchantId());
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
        }

        removeById(id);

        productAsyncService.clearProductCache(id);
        productAsyncService.clearProductListCache();
        log.info("[商品-删除成功] 商品ID:{}", id);
    }
}