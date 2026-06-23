package com.atlas.auth.service;

import com.atlas.auth.config.security.oauth2.OAuth2ProviderAuthenticationToken;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.model.TokenResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/8 14:58
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubLoginProvider extends AbstractThirdPartyLoginProvider {

    @Resource
    protected OAuth2ProviderEngine oAuth2ProviderEngine;

    @Override
    public String getProviderName() {
        return "github";
    }

    @Override
    public SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthAction action, Map<String, String> extraParams) {
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OAUTH2);
        String state = generateState(action);
        extraParams = extraParams == null ? new HashMap<>() : new HashMap<>(extraParams);
        extraParams.put("state", state);
        return oAuth2ProviderEngine.buildAuthorizeUrl(auth2ProviderSettings, extraParams);
    }

    @Override
    public TokenResponse authenticate(Authentication authentication) {
        OAuth2ProviderAuthenticationToken authenticationToken = (OAuth2ProviderAuthenticationToken) authentication;
        return processCallback(authenticationToken.code(),authenticationToken.state(),authenticationToken.codeVerifier());
    }

    public TokenResponse processCallback(String code, String state, String codeVerifier) {
        String providerName = getProviderName();
        log.info("Processing Github OAuth2 callback. provider: {}, state: {}, code: {}, codeVerifier: {}",
                providerName, state, code, codeVerifier);

        // 校验state
        ThirdPartyStateContext stateContext = validateState(state);

        // 获取配置
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.OAUTH2);

        // 获取token
        GitHubTokenResponse gitHubTokenResponse = oAuth2ProviderEngine.fetchToken(providerName, auth2ProviderSettings, code, codeVerifier, GitHubTokenResponse.class);
        log.info("GitHubTokenResponse: {}",gitHubTokenResponse);

        // 根据token获取用户信息
        OAuth2ProviderToken oAuth2ProviderToken = new OAuth2ProviderToken(gitHubTokenResponse.accessToken, gitHubTokenResponse.tokenType);
        GitHubUserInfoResponse gitHubUserInfoResponse = oAuth2ProviderEngine.fetchUserInfo(providerName, auth2ProviderSettings, oAuth2ProviderToken, GitHubUserInfoResponse.class);
        log.info("GitHubUserInfoResponse : {}", gitHubUserInfoResponse);

        // 根据token获取用户邮箱
        String url = auth2ProviderSettings.endpoints().userEmail().url();
        List<GitHubUserEmailResponse> gitHubUserEmailResponses = oAuth2ProviderEngine.getResource(providerName, url, oAuth2ProviderToken, new ParameterizedTypeReference<>() {});
        log.info("GitHubUserEmailResponses : {}", gitHubUserEmailResponses);

        GitHubUserEmailResponse primaryEmail = gitHubUserEmailResponses.stream()
                // 核心安全校验：必须是已经验证通过的邮箱
                .filter(GitHubUserEmailResponse::verified)
                // 优先筛选主邮箱
                .filter(GitHubUserEmailResponse::primary)
                .findFirst()
                // 降级策略：如果没有满足既验证又是primary的，退而求其次，找任何一个已验证的邮箱
                .orElseGet(() -> gitHubUserEmailResponses.stream()
                        .filter(GitHubUserEmailResponse::verified)
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("GitHub 账户未绑定任何已验证的邮箱，请先去 GitHub 验证邮件")));

        Map<String, Object> extraInfo = JsonUtils.convert(gitHubUserInfoResponse, new TypeReference<>() {});
        OAuth2UserInfo externalIdentityDTO = OAuth2UserInfo
                .builder()
                .sub(gitHubUserInfoResponse.id.toString())
                .provider(providerName)
                .fullName(gitHubUserInfoResponse.name)
                .avatar(gitHubUserInfoResponse.avatarUrl)
                .email(primaryEmail.email)
                .emailVerified(primaryEmail.verified)
                .extraInfo(extraInfo)
                .build();
        return dispatchFederatedIdentity(externalIdentityDTO, stateContext);
    }

    /**
     * GitHub OAuth2 Token 响应对象
     * * @author ys
     *
     * @date 2026/05/15
     */
    public record GitHubTokenResponse(

            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("token_type")
            String tokenType,

            String scope
    ) implements Serializable {
    }

    /**
     * GitHub 用户信息响应对象
     * * @author ys
     *
     * @date 2026/05/15
     */
    public record GitHubUserInfoResponse(
            String login,

            Long id,

            String name,

            @JsonProperty("node_id")
            String nodeId,

            @JsonProperty("avatar_url")
            String avatarUrl,

            @JsonProperty("gravatar_id")
            String gravatarId,

            String url,

            @JsonProperty("html_url")
            String htmlUrl,

            @JsonProperty("followers_url")
            String followersUrl,

            @JsonProperty("following_url")
            String followingUrl,

            @JsonProperty("gists_url")
            String gistsUrl,

            @JsonProperty("starred_url")
            String starredUrl,

            @JsonProperty("subscriptions_url")
            String subscriptionsUrl,

            @JsonProperty("organizations_url")
            String organizationsUrl,

            @JsonProperty("repos_url")
            String reposUrl,

            @JsonProperty("events_url")
            String eventsUrl,

            @JsonProperty("received_events_url")
            String receivedEventsUrl,

            String email
    ) implements Serializable {
    }

    public record GitHubUserEmailResponse(
            String email,

            Boolean primary,

            Boolean verified,

            String visibility
    ) implements Serializable {
    }

}
