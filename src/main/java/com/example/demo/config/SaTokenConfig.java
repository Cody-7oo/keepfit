package com.example.demo.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SaTokenConfig {

    // 用户端：type = "user"
    @Bean
    @Primary
    public StpLogic stpUserLogic() {
        return new StpLogic("user");
    }

    // 商家端：type = "merchant"
    @Bean
    public StpLogic stpMerchantLogic() {
        return new StpLogic("merchant");
    }
}