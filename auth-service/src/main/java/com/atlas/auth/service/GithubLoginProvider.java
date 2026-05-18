package com.atlas.auth.service;

import com.atlas.auth.config.properties.GithubOauth2Properties;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.model.TokenResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
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

    private final GithubOauth2Properties githubOauth2Properties;

    private final RestClient proxyRestClient;

    @Override
    public String getProviderName() {
        return githubOauth2Properties.getClientName();
    }

    @Override
    public String getAuthorizeUrl() {
        String authorizeCodeEndpoint = githubOauth2Properties.getAuthorizeCodeEndpoint();
        String clientId = githubOauth2Properties.getClientId();
        String redirectUri = githubOauth2Properties.getRedirectUrl();
        String scope = githubOauth2Properties.getScope();
        return UriComponentsBuilder.fromUriString(authorizeCodeEndpoint)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", "Github")
                .queryParam("prompt", "true")
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public TokenResponse processCallback(String code, String state, String codeVerifier) {
        log.info("Processing Github OAuth2 callback. Client: {}, Code: {}",
                githubOauth2Properties.getClientName(), code);
        //获取token
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", githubOauth2Properties.getClientId());
        body.add("client_secret", githubOauth2Properties.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", githubOauth2Properties.getRedirectUrl());
        if (StringUtils.hasText(codeVerifier)) {
            body.add("code_verifier", codeVerifier);
        }
        GitHubTokenResponse gitHubTokenResponse = proxyRestClient
                .post()
                .uri(githubOauth2Properties.getTokenEndpoint())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED) // 必须是表单格式
                .body(body)
                .retrieve()
                .body(GitHubTokenResponse.class);
        log.info("GitHubTokenResponse: {}", gitHubTokenResponse);
        // 用户信息
        GitHubUserInfoResponse gitHubUserInfoResponse = proxyRestClient
                .post()
                .uri(githubOauth2Properties.getUserInfoEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + gitHubTokenResponse.accessToken)
                .retrieve()
                .body(GitHubUserInfoResponse.class);
        log.info("GitHubUserInfoResponse : {}", gitHubUserInfoResponse);
        // 获取邮箱
        List<GitHubUserEmailResponse> gitHubUserEmailResponses = proxyRestClient
                .get()
                .uri(githubOauth2Properties.getUserEmailsEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + gitHubTokenResponse.accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
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
        ExternalIdentityDTO externalIdentityDTO = ExternalIdentityDTO
                .builder()
                .sub(gitHubUserInfoResponse.id.toString())
                .provider(githubOauth2Properties.getClientName())
                .fullName(gitHubUserInfoResponse.name)
                .avatar(gitHubUserInfoResponse.avatarUrl)
                .email(primaryEmail.email)
                .emailVerified(primaryEmail.verified)
                .extraInfo(extraInfo)
                .build();

        return doLogin(externalIdentityDTO);
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
