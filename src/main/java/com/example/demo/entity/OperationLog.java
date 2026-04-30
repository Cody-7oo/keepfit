package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 操作模块 */
    private String module;
    /** 操作类型 */
    private String operation;
    /** 请求地址 */
    private String requestUrl;
    /** 请求方式 */
    private String requestMethod;
    /** IP地址 */
    private String ip;
    /** 操作时间 */
    private LocalDateTime createTime;
}