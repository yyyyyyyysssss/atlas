package com.atlas.auth.config.security.totp;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/27 14:43
 */
@Configuration
public class TotpConfig {

    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        // 构建全局配置项
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                // 时间步长，默认是 30 秒
                .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30))
                .setWindowSize(1)
                .build();

        return new GoogleAuthenticator(config);
    }

    @Bean
    public GoogleAuthenticatorConfig googleAuthenticatorConfig() {
        return new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30)) // TOTP 一次有效的时间周期
                .setWindowSize(1) // 容错1个时间片，±30秒
                .build();
    }

}
