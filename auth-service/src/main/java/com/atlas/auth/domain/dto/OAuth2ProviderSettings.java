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
) implements SsoSettings, Decryptable<OAuth2ProviderSettings>, BaseUrlAppliable<OAuth2ProviderSettings> {

    private static final Pattern ENV_PATTERN = Pattern.compile("^\\$\\{(.+?)}$");

    public OAuth2ProviderSettings applyBaseUrl(String baseUrl) {
        if (this.endpoints == null) return this;
        return new OAuth2ProviderSettings(
                this.clientName,
                this.clientId,
                this.clientSecret,
                this.redirectUrl,
                this.scope,
                this.endpoints.withBaseUrl(baseUrl), // 驱动内部处理
                this.pkceRequired,
                this.extraParams
        );
    }


    public record Endpoints(
            EndpointConfig token,
            EndpointConfig userInfo,
            EndpointConfig authorizeCode,
            EndpointConfig userEmail,
            EndpointConfig qrScan
    ) {

        public Endpoints withBaseUrl(String baseUrl) {
            return new Endpoints(
                    this.token == null ? null : this.token.withBaseUrl(baseUrl),
                    this.userInfo == null ? null : this.userInfo.withBaseUrl(baseUrl),
                    this.authorizeCode == null ? null : this.authorizeCode.withBaseUrl(baseUrl),
                    this.userEmail == null ? null : this.userEmail.withBaseUrl(baseUrl),
                    this.qrScan == null ? null : this.qrScan.withBaseUrl(baseUrl)
            );
        }

    }

    public record EndpointConfig(
            String url,
            String method
    ) {
        private static final String BASE_URL_PLACEHOLDER = "{{baseUrl}}";

        public EndpointConfig withBaseUrl(String baseUrl) {
            // 如果没有显式包含该占位符，说明不需要动态拼接（例如是纯绝对路径），直接原样返回
            if (this.url == null || this.url.isBlank() || !this.url.contains(BASE_URL_PLACEHOLDER)) {
                return this;
            }

            // 规范化传入的 baseUrl（防呆处理：去掉末尾的斜杠）
            String cleanBaseUrl = baseUrl == null ? "" : baseUrl.trim();
            if (cleanBaseUrl.endsWith("/")) {
                cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
            }

            // 纯粹的占位符文本替换
            String resolvedUrl = this.url.replace(BASE_URL_PLACEHOLDER, cleanBaseUrl);

            return new EndpointConfig(resolvedUrl, this.method);
        }

    }

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
