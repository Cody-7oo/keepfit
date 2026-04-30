package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RateLimitConfig {

    /**
     * 加载限流Lua脚本
     */
    @Bean
    public DefaultRedisScript<Long> limitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);

        // 加载resources/lua/limit.lua脚本
        Resource resource = new DefaultResourceLoader().getResource("lua/limit.lua");
        script.setScriptSource(new ResourceScriptSource(resource));

        return script;
    }
}