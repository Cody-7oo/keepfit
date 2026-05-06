package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.handler.SensitiveDataHandler;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体
 * 企业规范：
 * 1. 字段与数据库完全映射
 * 2. 自动填充创建/更新时间
 * 3. 字段注释清晰
 * 4. 运动等级支持 1-5 级
 */
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer isDeleted;
    /**
     * 主键ID 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField(typeHandler = SensitiveDataHandler.class)
    private String username;

    /**
     * 账户余额（必须用BigDecimal，企业严禁用Double存金额）
     */
    private BigDecimal balance;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 登录密码（BCrypt加密存储）
     */
    private String password;

    /**
     * 手机号（登录账号，唯一）
     */
//    @TableField(typeHandler = SensitiveDataHandler.class)
    private String phone;

    /**
     * 默认收货地址
     */
    @TableField(typeHandler = SensitiveDataHandler.class)
    private String address;

    /**
     * 身高（cm）
     */
    private Double height;

    /**
     * 体重（kg）
     */
    private Double weight;

    /**
     * 运动等级（1-5级，1最低 5最高）
     * 1: 久坐
     * 2: 轻度活动
     * 3: 中度活动
     * 4: 重度活动
     * 5: 专业训练
     */
    private Integer sportLevel;

    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}