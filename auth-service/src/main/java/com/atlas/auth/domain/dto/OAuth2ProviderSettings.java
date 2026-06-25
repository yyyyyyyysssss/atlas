package com.atlas.auth.domain.dto;

import com.atlas.security.utils.AesUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record OAuth2ProviderSettings(
        String clientName,
        String clientId,
        String clientSecret,
        String redirectUrl,
        String scope,
        Endpoints endpoints,
        boolean pkceRequired,
        ExtraParams extraParams
) implements SsoSettings, Decryptable<OAuth2ProviderSettings> {

    private static final Pattern ENV_PATTERN = Pattern.compile("^\\$\\{(.+?)}$");

    public record Endpoints(
            EndpointConfig token,
            EndpointConfig userInfo,
            EndpointConfig authorizeCode,
            EndpointConfig userEmail,
            EndpointConfig qrScan
    ) {}

    public record EndpointConfig(
            String url,
            String method
    ) {}

    public record ExtraParams(
            Map<String, String> authorize,
            Map<String, String> token,
            Map<String, String> crypto
    ) {}

    @Override
    public OAuth2ProviderSettings decrypt(String key) {
        String finalSecret = this.clientSecret;
        if (finalSecret != null) {
            Matcher matcher = ENV_PATTERN.matcher(finalSecret.trim());
            if (matcher.matches()) {
                String envKey = matcher.group(1);
                finalSecret = System.getenv(envKey);
                if (finalSecret == null || finalSecret.isEmpty()) {
                    throw new IllegalStateException("未找到名为 [" + envKey + "] 的系统环境变量配置！");
                }
            } else {
                finalSecret = AesUtils.decrypt(finalSecret, key);
            }
        }
        return new OAuth2ProviderSettings(
                this.clientName,
                this.clientId,
                finalSecret,
                this.redirectUrl,
                this.scope,
                this.endpoints,
                this.pkceRequired,
                this.extraParams
        );
    }
}
