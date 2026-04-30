package com.example.demo.common.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

/**
 * 企业级敏感字段 AES 加密工具
 * 数据库存储密文，业务层自动解密
 */
public class AesEncryptUtil {

    /**
     * 密钥必须 16 位长度（企业规范）
     * 【重要】真实上线请放到配置中心，不要写死在代码！
     */
    private static final String KEY = "demo123456789012";
    private static final AES AES = SecureUtil.aes(KEY.getBytes(CharsetUtil.CHARSET_UTF_8));

    /**
     * 加密
     */
    public static String encrypt(String data) {
        if (StrUtil.isBlank(data)) {
            return null;
        }
        return AES.encryptHex(data);
    }

    /**
     * 解密
     */
    public static String decrypt(String data) {
        if (StrUtil.isBlank(data)) {
            return null;
        }
        try {
            return AES.decryptStr(data);
        } catch (Exception e) {
            return data;
        }
    }
}