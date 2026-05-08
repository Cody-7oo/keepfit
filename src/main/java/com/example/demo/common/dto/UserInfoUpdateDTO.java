package com.example.demo.common.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class UserInfoUpdateDTO {
    @NotNull(message = "用户id不能为空")
    private Long id;

    @NotNull(message = "昵称不能为空")
    private String username;
    //private String avatar;

    @NotNull(message = "身高不能为空")
    private Double height;

    @NotNull(message = "体重不能为空")
    private Double weight;

    @NotNull(message = "运动频率不能为空")
    private Integer sportLevel;
}