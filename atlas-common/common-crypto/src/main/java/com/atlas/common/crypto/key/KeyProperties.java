package com.atlas.common.crypto.key;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/17 14:37
 */
@Data
@ConfigurationProperties(prefix = "atlas.crypto")
public class KeyProperties {

    /**
     * 系统主密钥
     */
    private String masterKey;

}
