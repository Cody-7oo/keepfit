package com.example.demo;

/**
 * 放行接口文档的白名单
 */
public class WhiteList {

    // 接口文档需要放行的所有路径
    public static final String[] DOC_URLS = {
            "/doc.html",
            "/webjars/**",
            "/swagger-resources/**",
            "/v2/api-docs",
            "/v2/api-docs/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/favicon.ico",
            "/actuator/**"
    };

    /**
     * 判断当前请求是否是接口文档
     */
    public static boolean isDocUrl(String uri) {
        for (String pattern : DOC_URLS) {
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (uri.startsWith(prefix)) {
                    return true;
                }
            } else {
                if (uri.equals(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }
}