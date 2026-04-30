package com.example.demo.exception;

import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.result.R;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局参数 & 自定义异常处理器
 */
@RestControllerAdvice
public class ValidationExceptionHandler {  // 🔥 这里改名了！不再冲突！

    /**
     * 捕获自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> businessExceptionHandler(BusinessException e){
        return R.fail(e.getCode(), e.getMsg());
    }

    /**
     * 捕获 参数校验异常（@Valid 校验失败）
     */
    @ExceptionHandler(BindException.class)
    public R<?> bindExceptionHandler(BindException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return R.fail(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 捕获系统未知异常
     */
    @ExceptionHandler(Exception.class)
    public R<?> exceptionHandler(Exception e){
        e.printStackTrace();
        return R.fail(500,"系统繁忙，请稍后重试");
    }
}