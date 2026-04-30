package com.example.demo.common.result;

import com.example.demo.common.enums.ResultCodeEnum;
import lombok.Data;

/**
 * 全局统一返回结果
 * 企业级标准：code、msg、data 三段式 + isSuccess() 判断
 */
@Data
public class R<T> {
    private Integer code;
    private String msg;
    private T data;

    // 成功-无数据
    public static <T> R<T> ok() {
        return result(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMsg(), null);
    }

    // 成功-带数据
    public static <T> R<T> ok(T data) {
        return result(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMsg(), data);
    }

    // 成功-自定义提示 + 带数据
    public static <T> R<T> ok(String msg, T data) {
        return result(ResultCodeEnum.SUCCESS.getCode(), msg, data);
    }

    // 失败-自定义提示
    public static <T> R<T> fail(String msg) {
        return result(ResultCodeEnum.SYSTEM_ERROR.getCode(), msg, null);
    }

    // 失败-自定义状态码+提示
    public static <T> R<T> fail(Integer code, String msg) {
        return result(code, msg, null);
    }

    // 接收枚举
    public static <T> R<T> fail(ResultCodeEnum codeEnum) {
        return result(codeEnum.getCode(), codeEnum.getMsg(), null);
    }

    private static <T> R<T> result(Integer code, String msg, T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    /**
     * 判断请求是否成功（大厂标准）
     * 只要code == 200，就认为成功
     */
    public boolean isSuccess() {
        return ResultCodeEnum.SUCCESS.getCode().equals(this.code);
    }

    /**
     * 判断请求是否失败
     */
    public boolean isFail() {
        return !isSuccess();
    }
}