package com.example.demo.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class MerchantLoginDTO {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}