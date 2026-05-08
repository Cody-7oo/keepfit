package com.example.demo.controller.user;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.*;
import com.example.demo.config.SaTokenConfig;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.dto.UserInfoUpdateDTO;
import com.example.demo.common.dto.UserLoginDTO;
import com.example.demo.common.dto.UserRegisterDTO;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.result.R;
import com.example.demo.common.vo.UserVO;
import com.example.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import cn.dev33.satoken.stp.StpLogic;
import com.example.demo.config.SaTokenConfig;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/user")
@SaCheckLogin(type = "user")
public class UserController {

    @Resource
    private UserService userService;

    private Long getLoginUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @SaIgnore
    @RepeatSubmit
    @PostMapping("/register")
    @RateLimit(limit = 3, second = 10)
//    @DataScope(scopeType = "user")
//    @ApiSignature
//    @AntiReplay
    public R<Void> register(@RequestBody @Valid UserRegisterDTO dto) {
        log.info("[用户注册] 入参：{}", dto);
        long start = System.currentTimeMillis();
        try {
            userService.register(dto);
            log.info("[业务埋点-用户注册成功] 手机号:{}", dto.getPhone());
            return R.ok();
        } catch (Exception e) {
            log.info("[业务埋点-用户注册失败] 手机号:{}, 失败原因:{}", dto.getPhone(), e.getMessage());
            log.error("[用户注册] 异常：", e);
            throw e;
        } finally {
            log.info("[用户注册] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @SaIgnore
    @RepeatSubmit
    @PostMapping("/login")
    @RateLimit(limit = 3, second = 10)
    public R<UserVO> login(@RequestBody @Valid UserLoginDTO dto) {
        log.info("[用户登录] 手机号：{}", dto.getPhone());

        UserVO vo = userService.login(dto);

        StpLogic stpLogic = SaManager.getStpLogic("user");

        // 清除旧登录，生成新 token
        stpLogic.logout(vo.getId());
        stpLogic.login(vo.getId());

        // ====================== 🔥 权限存入【正确位置】 ======================
        stpLogic.getTokenSession().set("permissions", java.util.Arrays.asList(
                "user:order:create",
                "user:order:cancel",
                "user:order:myList"
        ));

        // ==================================================================

        String token = stpLogic.getTokenValue();
        vo.setToken(token);

        log.info("[用户登录] 成功，userId={}，token={}", vo.getId(), token);
        return R.ok(vo);
    }



    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    @RepeatSubmit
    @PostMapping("/update")
    @RateLimit(limit = 3, second = 10)
    public R<Void> update(@RequestBody @Valid UserInfoUpdateDTO dto, HttpServletRequest request) {
        try {
            // 1. 从请求头获取 token
            String token = request.getHeader("token");

            // 2. 直接获取你在配置类中注册的 user 账号 StpLogic 实例
            StpLogic stpLogic = new SaTokenConfig().stpUserLogic();

            // 3. 解析 token，获取用户ID（兼容旧版）
            Object loginIdObj = stpLogic.getLoginIdByToken(token);
            Long userId = Long.valueOf(loginIdObj.toString());

            // 4. 给上下文注入 token，让 @DataScope 能读到
            stpLogic.setTokenValue(token);

            // 5. 执行业务逻辑
            dto.setId(userId);
            userService.updateInfo(dto);

            return R.ok();
        } catch (Exception e) {
            log.error("用户信息更新失败", e);
            return R.fail(500, "更新失败：" + e.getMessage());
        }
    }

    @GetMapping("/getInfo")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    public R<UserVO> getInfo() {
        Long userId = getLoginUserId();
        log.info("[获取用户信息] 用户ID：{}", userId);
        long start = System.currentTimeMillis();
        try {
            return R.ok(userService.getUserInfo(userId));
        } catch (Exception e) {
            log.error("[获取用户信息] 异常：", e);
            throw e;
        } finally {
            log.info("[获取用户信息] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    @SaCheckLogin(type = "user")
    @GetMapping("/logout")
    @RateLimit(limit = 3, second = 10)
    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    public R<Void> logout() {
        long userId = getLoginUserId();
        StpUtil.logout();
        log.info("[用户退出登录] 用户ID：{} 成功", userId);
        log.info("[业务埋点-用户退出登录] userId:{}", userId);
        return R.ok();
    }

}