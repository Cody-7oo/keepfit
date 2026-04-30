package com.example.demo.common.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 优惠券前端展示VO
 * 屏蔽数据库多余字段、敏感字段
 */
@Data
public class CouponVO {

    private Long id;
    private Long templateId;
    private Integer status;
    private LocalDateTime receiveTime;
    private LocalDateTime useTime;
    private LocalDateTime expireTime;
}