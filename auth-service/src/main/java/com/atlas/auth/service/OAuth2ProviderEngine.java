package com.atlas.auth.service;

import com.atlas.auth.domain.dto.SsoProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.dto.OAuth2ProviderSettings;
import com.atlas.auth.domain.dto.OAuth2ProviderToken;
import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.security.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/17 9:35
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2ProviderEngine {

    private final RestClient proxyRestClient;

    private final RestClient defaultRestClient;

    private final SsoProviderService ssoProviderService;

    private final SecurityProperties securityProperties;

    // 组装授权 URL
    public SsoProviderAuthorizeUrlResponse buildAuthorizeUrl(OAuth2ProviderSettings settings, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(settings.endpoints().authorizeCode().url());
        return appendBaseParams(builder, settings, params);
    }

    // 扫码登录复用oauth2的授权码模式
    public SsoProviderAuthorizeUrlResponse buildQrScanUrl(OAuth2ProviderSettings settings) {

        return buildQrScanUrl(settings, Map.of());
    }

    public SsoProviderAuthorizeUrlResponse buildQrScanUrl(OAuth2ProviderSettings settings, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(settings.endpoints().qrScan().url());
        return appendBaseParams(builder, settings, params);
    }

    public <T> T fetchToken(String provider, OAuth2ProviderSettings settings, String code, String codeVerifier, Class<T> responseClass) {

        return fetchToken(provider, settings, code, codeVerifier, ParameterizedTypeReference.forType(responseClass));
    }

    public <T> T fetchToken(String provider, OAuth2ProviderSettings settings, String code, String codeVerifier, ParameterizedTypeReference<T> responseType) {
        String redirectUrl = securityProperties.getUiUrl() + settings.redirectUrl();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", settings.clientId());
        body.add("client_secret", settings.clientSecret());
        body.add("code", code);
        body.add("redirect_uri", redirectUrl);
        body.add("grant_type", "authorization_code");
        // 注入 extraParams 中的 token 参数
        if (settings.extraParams() != null && settings.extraParams().token() != null) {
            settings.extraParams().token().forEach(body::add);
        }
        if(settings.pkceRequired() && !StringUtils.hasText(codeVerifier)){
            log.error("[{}] 安全校验失败：开启了 PKCE 但缺失 code_verifier", provider);
            throw new BusinessException("认证凭证缺失 (code_verifier)");
        }
        if (StringUtils.hasText(codeVerifier)) {
            body.add("code_verifier", codeVerifier);
        }
        OAuth2ProviderSettings.EndpointConfig endpoint = settings.endpoints().token();
        RestClient restClient = getClient(provider);
        var spec = "POST".equalsIgnoreCase(endpoint.method())
                ? restClient.post().uri(endpoint.url())
                : restClient.get().uri(endpoint.url());
        if ("POST".equalsIgnoreCase(endpoint.method())) {
            ((RestClient.RequestBodySpec) spec)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body);
        }
        return spec
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String errorBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    log.error("[{}] 授权验证接口请求失败 [URL: {}, Status: {}, Body: {}]",
                            provider, request.getURI(), response.getStatusCode(), errorBody);
                    throw new BusinessException("授权验证失败");
                })
                .body(responseType);
    }

    public <T> T fetchUserInfo(String provider, OAuth2ProviderSettings settings, OAuth2ProviderToken token, Class<T> responseClass) {

        return fetchUserInfo(provider, settings, token, ParameterizedTypeReference.forType(responseClass));
    }

    // 获取用户信息
    public <T> T fetchUserInfo(String provider, OAuth2ProviderSettings settings, OAuth2ProviderToken token, ParameterizedTypeReference<T> responseType) {
        OAuth2ProviderSettings.EndpointConfig endpoint = settings.endpoints().userInfo();
        RestClient restClient = getClient(provider);
        var spec = "POST".equalsIgnoreCase(endpoint.method())
                ? restClient.post().uri(endpoint.url())
                : restClient.get().uri(endpoint.url());
        return spec
                .header(HttpHeaders.AUTHORIZATION, token.toAuthorizationHeader())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String errorBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    log.error("[{}] 获取用户信息接口请求失败 [URL: {}, Status: {}, Body: {}]",
                            provider, request.getURI(), response.getStatusCode(), errorBody);
                    throw new BusinessException("获取用户信息失败");
                })
                .body(responseType);
    }

    public <T> T getResource(String provider, String url, OAuth2ProviderToken token, Class<T> responseClass) {
        return getResource(provider, url, token, ParameterizedTypeReference.forType(responseClass));
    }

    public <T> T getResource(String provider, String url, OAuth2ProviderToken token, ParameterizedTypeReference<T> responseType) {
        RestClient restClient = getClient(provider);
        var request = restClient.get().uri(url);
        if (token != null && StringUtils.hasText(token.accessToken())) {
            request.header(HttpHeaders.AUTHORIZATION, token.toAuthorizationHeader());
        }
        return request
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(responseType);
    }

    public <T> T postResource(String provider, String url, OAuth2ProviderToken token, Object body, Class<T> responseClass) {

        return postResource(provider, url, token, body, ParameterizedTypeReference.forType(responseClass));
    }

    /**
     * 使用 POST 获取资源
     */
    public <T> T postResource(String provider, String url, OAuth2ProviderToken token, Object body, ParameterizedTypeReference<T> responseType) {
        RestClient restClient = getClient(provider);
        var request = restClient.post().uri(url);
        if (token != null && StringUtils.hasText(token.accessToken())) {
            request.header(HttpHeaders.AUTHORIZATION, token.toAuthorizationHeader());
        }
        return request
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    private RestClient getClient(String provider) {
        SsoProvider ssoProvider = ssoProviderService.getProvider(provider);
        if (ssoProvider == null) {
            log.warn("未找到提供商配置: {}, 使用默认客户端", provider);
        }
        return (ssoProvider != null && ssoProvider.getUseProxy()) ? proxyRestClient : defaultRestClient;
    }

    private SsoProviderAuthorizeUrlResponse appendBaseParams(UriComponentsBuilder builder, OAuth2ProviderSettings settings, Map<String, String> params) {
        String redirectUrl = securityProperties.getUiUrl() + settings.redirectUrl();
        builder.queryParam("client_id", settings.clientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("scope", settings.scope());

        // 注入静态额外参数
        if (settings.extraParams() != null && settings.extraParams().authorize() != null) {
            settings.extraParams().authorize().forEach(builder::replaceQueryParam);
        }
        // 注入动态额外参数
        if (params != null && !params.isEmpty()) {
            params.forEach(builder::replaceQueryParam);
        }
        String uriString = builder.build().encode().toUriString();
        return new SsoProviderAuthorizeUrlResponse(uriString, settings.pkceRequired());
    }


}
