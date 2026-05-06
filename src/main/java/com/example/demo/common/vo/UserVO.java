package com.example.demo.common.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String phone;
   // private String avatar;

    // 健身平台专属
    private Integer height;
    private Integer weight;
    private Integer exerciseLevel;

    private String token;
}