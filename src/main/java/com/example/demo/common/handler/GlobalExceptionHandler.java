package com.example.demo.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.result.R;
import com.example.demo.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器（大厂标准）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 未登录异常（Sa-Token）
     */
    @ExceptionHandler(NotLoginException.class)
    public R<?> handleNotLogin(NotLoginException e) {
        log.error("【全局异常】未登录：{}", e.getMessage());
        return R.fail(ResultCodeEnum.NOT_LOGIN);
    }

    /**
     * 无权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public R<?> handleNotPermission(NotPermissionException e) {
        log.error("【全局异常】无权限：{}", e.getMessage());
        return R.fail(ResultCodeEnum.NO_PERMISSION);
    }

    /**
     * 业务异常（自定义）
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException e) {
        log.warn("【全局异常】业务异常：{}", e.getMessage());
        return R.fail(e.getCode(), e.getMsg());
    }

    /**
     * 系统异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("【全局异常】系统异常：", e);
        return R.fail(ResultCodeEnum.SYSTEM_ERROR);
    }
}