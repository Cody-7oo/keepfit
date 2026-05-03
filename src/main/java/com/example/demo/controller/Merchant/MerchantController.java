package com.example.demo.controller.Merchant;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.*;
import com.example.demo.common.dto.MerchantLoginDTO;
import com.example.demo.common.dto.MerchantRegisterDTO;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.MerchantVO;
import com.example.demo.service.MerchantService;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/merchant")
//@Api(tags = "商家模块")
public class MerchantController {

    @Resource
    private MerchantService merchantService;

    /**
     * 商家注册
     */
    @RepeatSubmit
    @PostMapping("/register")
//    @ApiOperation("商家注册")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "merchant")
    @ApiSignature
    @AntiReplay
    public R<Void> register(@RequestBody @Valid MerchantRegisterDTO dto) {
        log.info("[商家注册] 入参：{}", dto);
        long start = System.currentTimeMillis();
        try {
            merchantService.register(dto);
            log.info("[业务埋点-商家注册成功] 手机号:{}", dto.getPhone());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商家注册失败] 手机号:{}, 失败原因:{}", dto.getPhone(), e.getMessage());
            log.error("[商家注册] 异常：", e);
            throw e;
        } finally {
            log.info("[商家注册] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 商家登录
     */
    @RepeatSubmit
    @PostMapping("/login")
//    @ApiOperation("商家登录")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "merchant")
    @ApiSignature
    @AntiReplay
    public R<MerchantVO> login(@RequestBody @Valid MerchantLoginDTO dto) {
        log.info("[商家登录] 手机号：{}", dto.getPhone());
        long start = System.currentTimeMillis();
        try {
            MerchantVO vo = merchantService.login(dto);
            StpUtil.login(vo.getId(), "merchant");
            log.info("[商家登录] 成功，商家ID：{}", vo.getId());
            log.info("[业务埋点-商家登录成功] merchantId:{}, phone:{}", vo.getId(), dto.getPhone());
            return R.ok(vo);
        } catch (Exception e) {
            log.info("[业务埋点-商家登录异常] 手机号:{}, 异常原因:{}", dto.getPhone(), e.getMessage());
            log.error("[商家登录] 异常：", e);
            throw e;
        } finally {
            log.info("[商家登录] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 获取当前登录商家信息
     */
    @SaCheckLogin(type = "merchant")
    @GetMapping("/getInfo")
//    @ApiOperation("获取当前登录商家信息")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "merchant")
    @ApiSignature
    @AntiReplay
    public R<MerchantVO> getInfo() {
        Long merchantId = StpUtil.getLoginIdAsLong();
        log.info("[获取商家信息] 商家ID：{}", merchantId);
        long start = System.currentTimeMillis();
        try {
            MerchantVO vo = merchantService.getMerchantInfo(merchantId);
            return R.ok(vo);
        } catch (Exception e) {
            log.error("[获取商家信息] 异常：", e);
            throw e;
        } finally {
            log.info("[获取商家信息] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 退出登录
     */
    @SaCheckLogin(type = "merchant")
    @GetMapping("/logout")
//    @ApiOperation("商家退出登录")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "merchant")
    @ApiSignature
    @AntiReplay
    public R<Void> logout() {
        Long merchantId = StpUtil.getLoginIdAsLong();
        long start = System.currentTimeMillis();
        try {
            StpUtil.logout();
            log.info("[商家退出登录] 成功");
            log.info("[业务埋点-商家退出登录] merchantId:{}", merchantId);
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-商家退出登录异常] merchantId:{}, 异常原因:{}", merchantId, e.getMessage());
            log.error("[商家退出登录] 异常：", e);
            throw e;
        } finally {
            log.info("[商家退出登录] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

}