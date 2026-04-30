package com.example.demo.common.util;


import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * XSS 过滤工具
 */
public class XssUtil {

    /**
     * 过滤 xss 非法脚本标签
     */
    public static String clean(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        // 只保留安全标签，过滤 script、iframe 等恶意代码
        return Jsoup.clean(content, Safelist.basic());
    }
}