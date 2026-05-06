package com.example.demo.common.vo;

import com.example.demo.common.util.PhoneUtil;
import lombok.Data;

@Data
public class MerchantVO {
    private Long id;
    private String phone;
    private String merchantName;
    private Integer status;
    private String token;

    // 🔥 核心：重写 getPhone 方法，自动脱敏
    public String getPhone() {
        return PhoneUtil.maskPhone(this.phone);
    }
}