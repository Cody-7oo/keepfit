package com.example.demo.exception;

import com.example.demo.common.enums.ResultCodeEnum;
import lombok.Data;

/**
 * 自定义业务异常
 * 所有业务报错统一抛这个异常
 */
@Data
public class BusinessException extends RuntimeException{

    private Integer code;
    private String msg;

    public BusinessException(ResultCodeEnum resultCodeEnum){
        this.code = resultCodeEnum.getCode();
        this.msg = resultCodeEnum.getMsg();
    }

    public BusinessException(Integer code,String msg){
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(ResultCodeEnum resultCodeEnum, String message){
        this.code = resultCodeEnum.getCode();
        // 优先使用自定义消息
        this.msg = message;
    }
}