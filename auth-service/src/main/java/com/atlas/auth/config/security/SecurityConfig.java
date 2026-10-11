package com.atlas.auth.config.security;


import com.atlas.auth.config.security.handler.LoginAttemptHandler;
import com.atlas.auth.config.security.mfa.MfaTicketRepository;
import com.atlas.auth.config.security.mfa.MfaVerifyStrategyFactory;
import com.atlas.auth.config.security.provider.*;
import com.atlas.auth.config.security.saml2.*;
import com.atlas.auth.config.security.service.HeaderBasedRememberMeServices;
import com.atlas.auth.config.security.webauthn.AtlasPublicKeyCredentialUserEntityRepository;
import com.atlas.auth.service.*;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.common.security.handler.ForbiddenAccessHandler;
import com.atlas.common.security.handler.UnauthorizedEntryPoint;
import com.atlas.common.security.oauth2.OAuth2BearerTokenResolver;
import com.atlas.common.security.properties.SecurityProperties;
import com.atlas.common.security.service.TokenService;
import jakarta.annotation.Resource;
import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ott.JdbcOneTimeTokenService;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationProvider;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;

@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityConfig {

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private OneTimeTokenGenerationSuccessService oneTimeTokenGenerationSuccessService;

    @Resource
    private SecurityContextRepository redisSecurityContextRepository;

    @Resource
    private Saml2SuccessHandler saml2SuccessHandler;

    @Resource
    private Saml2FailureHandler saml2FailureHandler;

    @Resource
    private DelegateRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    @Resource
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Resource
    private OAuth2BearerTokenResolver oAuth2BearerTokenResolver;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, Saml2IdpFilter saml2IdpFilter, OneTimeTokenService oneTimeTokenService, LogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults())
                .exceptionHandling(exception -> {
                    exception.authenticationEntryPoint(new UnauthorizedEntryPoint());
                    exception.accessDeniedHandler(new ForbiddenAccessHandler());
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize
                            //允许所有人访问的路径
                            .requestMatchers(securityProperties.getAuthorize().getPermit().toArray(new String[0])).permitAll()
                            //允许所有异步请求
                            .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                            // 必须通过身份认证
                            .anyRequest().authenticated();
                })
                .saml2Login(saml2 -> saml2
                        // 会自动匹配 application.yml 里的 auth0
                        .loginProcessingUrl("/saml2/sso/{registrationId}")
                        .successHandler(saml2SuccessHandler)
                        .failureHandler(saml2FailureHandler)
                        .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository)
                )
                //一次性令牌
                .oneTimeTokenLogin((ott) -> {
                    //生成一次性令牌的路径
                    ott.tokenGeneratingUrl("/ott/generate");
                    //登录处理路径
                    ott.loginProcessingUrl("/login/ott");
                    //禁用默认提交页面
                    ott.showDefaultSubmitPage(false);
                    //令牌生成以及存储
                    ott.tokenService(oneTimeTokenService);
                    //令牌生成成功处理器
                    ott.tokenGenerationSuccessHandler(oneTimeTokenGenerationSuccessService);
                })
                .securityContext(securityContext -> {
                    securityContext.securityContextRepository(redisSecurityContextRepository);
                })
                // saml2 idp
                .addFilterBefore(saml2IdpFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout ->
                        logout
                                .logoutUrl("/logout")
                                .logoutSuccessHandler(logoutSuccessHandler)
                                .permitAll()
                )
                // oauth2资源服务器
                .oauth2ResourceServer((resourceServer) -> {
                    resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter));
                    resourceServer.bearerTokenResolver(oAuth2BearerTokenResolver);
                });


        return http.build();
    }

    //身份认证管理器
    @Bean
    @Primary
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       DaoAuthenticationProvider daoAuthenticationProvider,
                                                       CaptchaAuthenticationProvider captchaAuthenticationProvider,
                                                       ThirdPartyAuthenticationProvider thirdPartyAuthenticationProvider,
                                                       RefreshAuthenticationProvider refreshAuthenticationProvider,
                                                       RememberMeAuthenticationProvider rememberMeAuthenticationProvider,
                                                       OneTimeTokenAuthenticationProvider oneTimeTokenAuthenticationProvider,
                                                       WebauthnAuthenticationProvider webauthnAuthenticationProvider,
                                                       Web3WalletAuthenticationProvider web3WalletAuthenticationProvider,
                                                       Saml2IdpAuthenticationProvider saml2IdpAuthenticationProvider,
                                                       MfaAuthenticationProvider mfaAuthenticationProvider) throws Exception {

        return http.getSharedObject(AuthenticationManagerBuilder.class)
                //用户名密码身份认证
                .authenticationProvider(daoAuthenticationProvider)
                // 验证码认证
                .authenticationProvider(captchaAuthenticationProvider)
                //用于使用三方登录的身份认证
                .authenticationProvider(thirdPartyAuthenticationProvider)
                // 刷新令牌
                .authenticationProvider(refreshAuthenticationProvider)
                //记住我身份认证
                .authenticationProvider(rememberMeAuthenticationProvider)
                //一次性令牌认证
                .authenticationProvider(oneTimeTokenAuthenticationProvider)
                //webauthn通行密钥认证
                .authenticationProvider(webauthnAuthenticationProvider)
                // web3认证
                .authenticationProvider(web3WalletAuthenticationProvider)
                // saml idp
                .authenticationProvider(saml2IdpAuthenticationProvider)
                // mfa双因子认证
                .authenticationProvider(mfaAuthenticationProvider)
                .parentAuthenticationManager(null)
                .build();
    }

    //基于用户名密码认证
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(RedisHelper redisHelper, UserService userService, PasswordEncoder passwordEncoder) {
        // 密码防暴力破解登录
        LoginAttemptHandler loginAttemptService = new LoginAttemptHandler(redisHelper);
        DaoAuthenticationProvider authProvider = new UsernamePasswordAuthenticationProvider(userService, loginAttemptService);
        // 设置密码编辑器
        authProvider.setPasswordEncoder(passwordEncoder);
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public RememberMeAuthenticationProvider rememberMeAuthenticationProvider(SecurityProperties securityProperties) {
        String secretKey = securityProperties.getRememberMe().getSecretKey();
        return new RememberMeAuthenticationProvider(secretKey);
    }

    //记住我
    @Bean
    public RememberMeAuthenticationFilter rememberMeFilter(AuthenticationManager authenticationManager, RememberMeServices rememberMeServices) {

        return new RememberMeAuthenticationFilter(authenticationManager, rememberMeServices);
    }

    @Bean
    public RememberMeServices rememberMeServices(SecurityProperties securityProperties, TokenService tokenService, UserService userService) {
        String secretKey = securityProperties.getRememberMe().getSecretKey();
        return new HeaderBasedRememberMeServices(secretKey, tokenService, userService);
    }

    // 验证码认证
    @Bean
    public CaptchaAuthenticationProvider captchaAuthenticationProvider(UserService userService, CaptchaFactory captchaFactory) {
        return new CaptchaAuthenticationProvider(userService, captchaFactory);
    }

    //一次性令牌
    //使用数据库存储
    @Bean
    public OneTimeTokenService oneTimeTokenService(JdbcTemplate jdbcTemplate) {
        return new JdbcOneTimeTokenService(jdbcTemplate);
    }

    @Bean
    public OneTimeTokenAuthenticationProvider oneTimeTokenAuthenticationProvider(OneTimeTokenService oneTimeTokenService, UserService userService) {
        return new OneTimeTokenAuthenticationProvider(oneTimeTokenService, userService);
    }

    // 通行密钥
    @Bean
    public PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository(UserService userService) {
        return new AtlasPublicKeyCredentialUserEntityRepository(userService);
    }

    @Bean
    public WebauthnAuthenticationProvider webauthnAuthenticationProvider(WebauthnService webauthnService, UserService userService) {

        return new WebauthnAuthenticationProvider(webauthnService, userService);
    }

    // web3凭证
    @Bean
    public Web3WalletAuthenticationProvider web3WalletAuthenticationProvider(UserService userService, Web3WalletService web3WalletService, UserWeb3CredentialsService userWeb3CredentialsService) {

        return new Web3WalletAuthenticationProvider(userService, web3WalletService, userWeb3CredentialsService);
    }

    //三方登录认证
    @Bean
    public ThirdPartyAuthenticationProvider thirdPartyAuthenticationProvider(UserService userService) {

        return new ThirdPartyAuthenticationProvider(userService);
    }

    @Bean
    public RefreshAuthenticationProvider refreshAuthenticationProvider(UserService userService, TokenService tokenService) {

        return new RefreshAuthenticationProvider(userService, tokenService);
    }

    @Bean
    public MfaAuthenticationProvider mfaAuthenticationProvider(UserService userService, MfaTicketRepository mfaTicketRepository, MfaVerifyStrategyFactory mfaVerifyStrategyFactory) {

        return new MfaAuthenticationProvider(
                userService,
                mfaTicketRepository,
                mfaVerifyStrategyFactory
        );
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler(LogoutService logoutService) {

        return logoutService;
    }

    // saml2 idp
    @Bean
    public Saml2IdpFilter saml2IdpFilter(AuthenticationManager authenticationManager, Saml2IdpService saml2IdpService) {

        return new Saml2IdpFilter(authenticationManager,saml2IdpService);
    }

    @Bean
    public Saml2IdpAuthenticationProvider saml2IdpAuthenticationProvider(Saml2IdpService saml2IdpService,
                                                                         Saml2RegisteredClientService saml2RegisteredClientService,
                                                                         SecurityProperties securityProperties,
                                                                         RedisHelper redisHelper) {

        return new Saml2IdpAuthenticationProvider(saml2IdpService, saml2RegisteredClientService, securityProperties, redisHelper);
    }

}
