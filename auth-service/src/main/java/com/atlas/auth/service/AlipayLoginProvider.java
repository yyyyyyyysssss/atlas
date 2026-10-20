package com.atlas.auth.service;

import com.alipay.v3.ApiClient;
import com.alipay.v3.api.AlipaySystemOauthApi;
import com.alipay.v3.api.AlipayUserInfoApi;
import com.alipay.v3.model.AlipaySystemOauthTokenModel;
import com.alipay.v3.model.AlipaySystemOauthTokenResponseModel;
import com.alipay.v3.model.AlipayUserInfoShareResponseModel;
import com.alipay.v3.util.model.AlipayConfig;
import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.auth.config.exception.ThirdPartyAuthException;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/25 14:45
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlipayLoginProvider extends AbstractThirdPartyLoginProvider{

    @Resource
    protected OAuth2ProviderEngine oAuth2ProviderEngine;

    @Override
    public String getProviderName() {
        return "alipay";
    }

    private final OkHttpClient sharedHttpClient = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthRequestContext requestContext, Map<String, String> extraParams) {
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OAUTH2);
        if (auth2ProviderSettings == null) {
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_NOT_FOUND, getProviderName(), SsoProviderProtocol.OAUTH2);
        }
        String state = generateState(requestContext);
        extraParams = extraParams == null ? new HashMap<>() : new HashMap<>(extraParams);
        extraParams.put("state", state);
        extraParams.put("app_id", auth2ProviderSettings.clientId());
        return oAuth2ProviderEngine.buildAuthorizeUrl(auth2ProviderSettings, extraParams);
    }

    @Override
    public ThirdPartyCallbackVO authenticate(ThirdPartyLoginRequest request) {
        OAuth2LoginRequest oauth2 = (OAuth2LoginRequest)request;
        return processCallback(oauth2.code(),oauth2.state());
    }

    public ThirdPartyCallbackVO processCallback(String code,String state) {
        String providerName = getProviderName();
        log.info("Processing Alipay OAuth2 callback provider: {}, state: {}, code: {}",
                providerName, state, code);

        // 校验state
        ThirdPartyStateContext stateContext = validateState(state);

        // 获取配置
        OAuth2ProviderSettings settings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.OAUTH2);
        if (settings == null) {
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_NOT_FOUND, getProviderName(), SsoProviderProtocol.OAUTH2);
        }
        OAuth2UserInfo oAuth2UserInfo;
        try {
            ApiClient apiClient = new ApiClient(sharedHttpClient);
            apiClient.setAlipayConfig(getAlipayConfig(settings));

            // 获取token
            AlipaySystemOauthApi tokenApi = new AlipaySystemOauthApi(apiClient);
            AlipaySystemOauthTokenModel data = new AlipaySystemOauthTokenModel();

            data.setCode(code);
            data.setGrantType("authorization_code");
            AlipaySystemOauthTokenResponseModel response = tokenApi.token(data);
            log.info("Processing Alipay OAuth2 callback. provider: {} tokenInfo: {}", providerName, response.toJson());

            // 获取用户信息
            AlipayUserInfoApi userInfoApi = new AlipayUserInfoApi(apiClient);
            AlipayUserInfoShareResponseModel userInfo = userInfoApi.share(response.getAccessToken());
            log.info("Processing Alipay OAuth2 callback. provider: {} userInfo: {}", providerName, userInfo.toJson());

            oAuth2UserInfo = new OAuth2UserInfo();
            oAuth2UserInfo.setProvider(providerName);
            oAuth2UserInfo.setSub(userInfo.getOpenId());
            oAuth2UserInfo.setFullName(userInfo.getNickName());
            oAuth2UserInfo.setAvatar(userInfo.getAvatar());
        }catch (Exception e){
            log.warn("[{}] OAUTH2 认证失败", getProviderName(), e);
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_AUTH_FAILED, getProviderName(), SsoProviderProtocol.OAUTH2);
        }
        return dispatchFederatedIdentity(oAuth2UserInfo, stateContext);
    }

    private AlipayConfig getAlipayConfig(OAuth2ProviderSettings settings) {
        String serverUrl = null;
        if (settings.extraParams() != null && settings.extraParams().token() != null) {
            String configuredUrl = settings.extraParams().token().get("serverUrl");
            if (configuredUrl != null && !configuredUrl.isEmpty()) {
                serverUrl = configuredUrl.trim();
            }
        }

        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_CONFIG_ERROR, getProviderName(), SsoProviderProtocol.OAUTH2);
        }

        String alipayPublicKey = null;
        if (settings.extraParams().crypto() != null) {
            alipayPublicKey = settings.extraParams().crypto().get("publicKey");
        }

        if (alipayPublicKey == null || alipayPublicKey.isEmpty()) {
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_CONFIG_ERROR, getProviderName(), SsoProviderProtocol.OAUTH2);
        }
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(serverUrl);
        alipayConfig.setAppId(settings.clientId());
        alipayConfig.setPrivateKey(settings.clientSecret());
        alipayConfig.setAlipayPublicKey(alipayPublicKey);
        return alipayConfig;
    }

}
