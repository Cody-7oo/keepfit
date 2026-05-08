package com.example.demo.controller.user;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.annotation.*;
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
import java.util.Enumeration;

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
        long start = System.currentTimeMillis();

        UserVO vo = userService.login(dto);

        // ====================== 🔥 正确多账号登录（固定写法） ======================
        StpLogic stpLogic = SaManager.getStpLogic("user");
        stpLogic.login(vo.getId()); // 登录 user 类型
        String token = stpLogic.getTokenValue(); // 取 user 类型的 token
        // =======================================================================

        System.out.println("============ 登录生成的 token = " + token);

        vo.setToken(token);

        log.info("[用户登录] 成功，用户ID：{}，token：{}", vo.getId(), token);
        log.info("[用户登录] 耗时：{}ms", System.currentTimeMillis() - start);
        return R.ok(vo);
    }


//    @DataScope(scopeType = "user")
    @ApiSignature
    @AntiReplay
    @RepeatSubmit
    @PostMapping("/update")
    @RateLimit(limit = 3, second = 10)
    public R<Void> update(@RequestBody @Valid UserInfoUpdateDTO dto, HttpServletRequest request) {
        String token = request.getHeader("token");
        try {
            // 🔥 🔥 🔥 旧版唯一不报错的多账户写法
            StpLogic stpLogic = SaManager.getStpLogic("user");

            // 只拿Object，绝对不调用getLoginIdAsLong！
            Object loginId = stpLogic.getLoginIdByToken(token);
            Long userId = Long.parseLong(loginId.toString());

            dto.setId(userId);
            userService.updateInfo(dto);

            return R.ok();
        } catch (Exception e) {
            log.error("错误：", e);
            return R.fail(500, "更新失败");
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