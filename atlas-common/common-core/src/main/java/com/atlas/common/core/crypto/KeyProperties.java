package com.atlas.common.core.crypto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/17 14:37
 */
@Data
@ConfigurationProperties(prefix = "atlas.crypto")
@Validated
public class KeyProperties {

    /**
     * 系统主密钥
     */
    @NotBlank(message = "atlas.crypto.master-key 不能为空")
    private String masterKey;

}
