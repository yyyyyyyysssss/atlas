package com.atlas.common.crypto.autoconfigure;

import com.atlas.common.core.autoconfigure.AtlasCoreAutoConfiguration;
import com.atlas.common.crypto.key.KeyDerivationService;
import com.atlas.common.crypto.key.KeyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/18 11:04
 */

@AutoConfiguration(after = {
        AtlasCoreAutoConfiguration.class,
})
@EnableConfigurationProperties(KeyProperties.class)
@RequiredArgsConstructor
public class CryptoAutoConfiguration {

    private final KeyProperties keyProperties;

    @Bean
    @ConditionalOnMissingBean // 允许特定的服务覆盖加密方案
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public KeyDerivationService keyDerivationService() {
        String masterKey = keyProperties.getMasterKey();
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException("atlas.crypto.master-key 未配置");
        }
        return new KeyDerivationService(masterKey);
    }


}
