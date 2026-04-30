package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.handler.SensitiveDataHandler;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家实体
 * 企业规范：注释完整、自动填充时间、字段规范、权限字段明确
 */
@Data
@TableName("merchant")
public class Merchant implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 逻辑删除
     */
    private Integer isDeleted;

    /**
     * 主键ID 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商家名称
     */
    private String merchantName;

    /**
     * 商家登录手机号
     */
    @TableField(typeHandler = SensitiveDataHandler.class)
    private String phone;

    /**
     * 登录密码（BCrypt加密）
     */
    private String password;

    /**
     * 状态 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}