package com.atlas.auth.config.security.oauth2;


import com.atlas.common.core.api.user.dto.AuthorityUrl;
import com.atlas.common.core.utils.RsaUtils;
import com.atlas.security.filter.TokenAuthenticationFilter;
import com.atlas.security.jackson.AuthorityUrlMixin;
import com.atlas.security.jackson.RequestUrlAuthorityMixin;
import com.atlas.security.jackson.SecurityUserMixin;
import com.atlas.security.model.RequestUrlAuthority;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.oauth2.OAuth2BearerTokenResolver;
import com.atlas.security.properties.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.jackson2.WebServletJackson2Module;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.util.DigestUtils;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@EnableWebSecurity
@Configuration
@Slf4j
public class OAuth2AuthorizationServerConfig {

    @Resource
    private OidcUserInfoService oidcUserInfoService;

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Resource
    private OAuth2BearerTokenResolver oAuth2BearerTokenResolver;

    @Resource
    private TokenAuthenticationFilter tokenAuthenticationFilter;

    @Resource
    private SecurityContextRepository redisSecurityContextRepository;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        // 配置默认的设置
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();
        //自定义 /userinfo响应的内容
        Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper = (context) -> {
            OidcUserInfoAuthenticationToken authentication = context.getAuthentication();
            JwtAuthenticationToken principal = (JwtAuthenticationToken) authentication.getPrincipal();
            return oidcUserInfoService.loadUser(principal.getName());
        };
        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer
                                .oidc((oidc) -> {
                                    oidc.userInfoEndpoint((userInfo) -> userInfo.userInfoMapper(userInfoMapper));
                                })
                                .authorizationEndpoint(authorizationEndpoint -> {
                                    authorizationEndpoint.consentPage("/oauth2/consent?type=code");
                                })
                                .deviceAuthorizationEndpoint(deviceAuthorizationEndpoint -> {
                                    deviceAuthorizationEndpoint.verificationUri(securityProperties.getUiUrl() + "/oauth2/activate");
                                })
                                .deviceVerificationEndpoint(deviceVerificationEndpoint -> {
                                    deviceVerificationEndpoint.consentPage("/oauth2/consent?type=device");
                                    deviceVerificationEndpoint.deviceVerificationResponseHandler(new SimpleUrlAuthenticationSuccessHandler(securityProperties.getUiUrl() + "/oauth2/activated"));
                                })
                )
                .authorizeHttpRequests((authorize) ->
                        authorize.anyRequest().authenticated()
                )
                // 当未登录时访问认证端点时重定向至login页面
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginTargetAuthenticationEntryPoint(securityProperties.getUiUrl() + "/login",securityProperties.getIssuerUrl() + "/api/auth"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .securityContext(securityContext -> {
                    securityContext.securityContextRepository(redisSecurityContextRepository);
                })
                .addFilterBefore(tokenAuthenticationFilter, SecurityContextHolderFilter.class)
                // oauth2资源服务器
                .oauth2ResourceServer((resourceServer) -> {
                    resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter));
                    resourceServer.bearerTokenResolver(oAuth2BearerTokenResolver);
                });


        return http.build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> oAuth2TokenCustomizer() {
        return context -> {
            String name = context.getPrincipal().getName();
            //自定义id_token中包含的信息
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                OidcUserInfo oidcUserInfo = oidcUserInfoService.loadUser(name);
                context.getClaims().claims(claims -> claims.putAll(oidcUserInfo.getClaims()));
            }
        };
    }

    // 注册客户端应用, 对应 oauth2_registered_client 表
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    // 令牌的发放记录, 对应 oauth2_authorization 表
    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        JdbcOAuth2AuthorizationService jdbcOAuth2AuthorizationService = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);

        JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper authorizationRowMapper = new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(
                registeredClientRepository);
        authorizationRowMapper.setLobHandler(new DefaultLobHandler());

        //spring security 反序列化设置
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new CoreJackson2Module());
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new WebServletJackson2Module());
        objectMapper.addMixIn(RequestUrlAuthority.class, RequestUrlAuthorityMixin.class);
        objectMapper.addMixIn(AuthorityUrl.class, AuthorityUrlMixin.class);
        objectMapper.addMixIn(SecurityUser.class, SecurityUserMixin.class);

        authorizationRowMapper.setObjectMapper(objectMapper);

        jdbcOAuth2AuthorizationService.setAuthorizationRowMapper(authorizationRowMapper);

        return jdbcOAuth2AuthorizationService;
    }

    // 把资源拥有者授权确认操作保存到数据库, 对应 oauth2_authorization_consent 表
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        JdbcOAuth2AuthorizationConsentService jdbcOAuth2AuthorizationConsentService = new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
        return new OAuth2AuthorizationConsentService(){

            @Override
            public void save(OAuth2AuthorizationConsent authorizationConsent) {
//                jdbcOAuth2AuthorizationConsentService.save(authorizationConsent);
            }

            @Override
            public void remove(OAuth2AuthorizationConsent authorizationConsent) {
                jdbcOAuth2AuthorizationConsentService.remove(authorizationConsent);
            }

            @Override
            public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
                OAuth2AuthorizationConsent consent = jdbcOAuth2AuthorizationConsentService.findById(registeredClientId, principalName);
                if (consent == null) {
                    RegisteredClient client = registeredClientRepository.findById(registeredClientId);
                    if (client != null && !client.getClientSettings().isRequireAuthorizationConsent()) {
                        Set<SimpleGrantedAuthority> authorities = client.getScopes().stream()
                                .map(m -> new SimpleGrantedAuthority("SCOPE_" + m))
                                .collect(Collectors.toSet());
                        return OAuth2AuthorizationConsent.withId(registeredClientId, principalName)
                                .authorities(auths -> auths.addAll(authorities))
                                .build();
                    }
                }
                return consent;
            }
        };
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer(securityProperties.getIssuerUrl()).build();
    }



    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        RSAPublicKey publicKey = (RSAPublicKey) RsaUtils.loadLocalPublicKey();
        RSAPrivateKey privateKey = (RSAPrivateKey) RsaUtils.loadLocalPrivateKey();
        String kid = DigestUtils.md5DigestAsHex(publicKey.getEncoded());
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(kid)
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

}
