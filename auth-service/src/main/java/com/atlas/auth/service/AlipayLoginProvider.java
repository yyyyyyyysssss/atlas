package com.atlas.auth.service;

import com.alipay.v3.ApiClient;
import com.alipay.v3.api.AlipaySystemOauthApi;
import com.alipay.v3.api.AlipayUserInfoApi;
import com.alipay.v3.model.AlipaySystemOauthTokenModel;
import com.alipay.v3.model.AlipaySystemOauthTokenResponseModel;
import com.alipay.v3.model.AlipayUserInfoShareResponseModel;
import com.alipay.v3.util.model.AlipayConfig;
import com.atlas.auth.config.security.oauth2.OAuth2ProviderAuthenticationToken;
import com.atlas.auth.domain.dto.OAuth2ProviderSettings;
import com.atlas.auth.domain.dto.OAuth2UserInfo;
import com.atlas.auth.domain.dto.SsoProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.dto.ThirdPartyStateContext;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.security.model.TokenResponse;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.security.core.Authentication;
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
    public SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthAction action, Map<String, String> extraParams) {
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OAUTH2);
        String state = generateState(action);
        extraParams = extraParams == null ? new HashMap<>() : new HashMap<>(extraParams);
        extraParams.put("state", state);
        extraParams.put("app_id", auth2ProviderSettings.clientId());
        return oAuth2ProviderEngine.buildAuthorizeUrl(auth2ProviderSettings, extraParams);
    }

    @Override
    public TokenResponse authenticate(Authentication authentication) {
        OAuth2ProviderAuthenticationToken authenticationToken = (OAuth2ProviderAuthenticationToken) authentication;
        return processCallback(authenticationToken.code(),authenticationToken.state());
    }

    public TokenResponse processCallback(String code,String state) {
        String providerName = getProviderName();
        log.info("Processing Alipay OAuth2 callback provider: {}, state: {}, code: {}",
                providerName, state, code);

        // 校验state
        ThirdPartyStateContext stateContext = validateState(state);

        // 获取配置
        OAuth2ProviderSettings settings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.OAUTH2);
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

            OAuth2UserInfo oAuth2UserInfo = new OAuth2UserInfo();
            oAuth2UserInfo.setProvider(providerName);
            oAuth2UserInfo.setSub(userInfo.getOpenId());
            oAuth2UserInfo.setFullName(userInfo.getNickName());
            oAuth2UserInfo.setAvatar(userInfo.getAvatar());

            return dispatchFederatedIdentity(oAuth2UserInfo, stateContext);
        }catch (Exception e){
            throw new BusinessException("支付宝登录失败: " + e.getMessage());
        }
    }

    private static AlipayConfig getAlipayConfig(OAuth2ProviderSettings settings) {
        String serverUrl = null;
        if (settings.extraParams() != null && settings.extraParams().token() != null) {
            String configuredUrl = settings.extraParams().token().get("serverUrl");
            if (configuredUrl != null && !configuredUrl.isEmpty()) {
                serverUrl = configuredUrl.trim();
            }
        }

        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalArgumentException("支付宝 serverUrl 未在配置中指定！");
        }

        String alipayPublicKey = null;
        if (settings.extraParams().crypto() != null) {
            alipayPublicKey = settings.extraParams().crypto().get("publicKey");
        }

        if (alipayPublicKey == null || alipayPublicKey.isEmpty()) {
            throw new IllegalArgumentException("支付宝公钥 (publicKey) 未在配置中指定！");
        }
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(serverUrl);
        alipayConfig.setAppId(settings.clientId());
        alipayConfig.setPrivateKey(settings.clientSecret());
        alipayConfig.setAlipayPublicKey(alipayPublicKey);
        return alipayConfig;
    }

}
