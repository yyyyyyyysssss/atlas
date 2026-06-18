package com.atlas.security.utils;

import java.util.HexFormat;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/18 10:03
 */
public class KeyManager {

    // 主密钥，从环境变量加载
    private static final String MASTER_KEY = System.getenv("ATLAS_MASTER_KEY");

    static {
        if (MASTER_KEY == null || MASTER_KEY.isEmpty()) {
            throw new IllegalStateException("系统启动失败：环境变量 ATLAS_MASTER_KEY 未配置！");
        }
    }


    public static String deriveServiceKey(String serviceName) {
        byte[] hash = DigestUtils.hmacBytes(serviceName, MASTER_KEY, "HmacSHA256");
        return HexFormat.of().formatHex(hash).substring(0, 32);
    }

}
