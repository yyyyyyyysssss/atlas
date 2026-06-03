package com.atlas.auth.config.security;


import com.atlas.auth.config.security.authentication.provider.*;
import com.atlas.auth.config.security.handler.LoginAttemptHandler;
import com.atlas.auth.config.security.mfa.MfaTicketRepository;
import com.atlas.auth.config.security.mfa.MfaVerifyStrategyFactory;
import com.atlas.auth.config.security.service.HeaderBasedRememberMeServices;
import com.atlas.auth.config.security.webauthn.AtlasPublicKeyCredentialUserEntityRepository;
import com.atlas.auth.service.*;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.filter.TokenAuthenticationFilter;
import com.atlas.security.handler.ForbiddenAccessHandler;
import com.atlas.security.handler.UnauthorizedEntryPoint;
import com.atlas.security.properties.SecurityProperties;
import com.atlas.security.service.TokenService;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;

@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityConfig {

    @Resource
    private LogoutService logoutService;

    @Resource
    private RedisHelper redisHelper;

    @Resource
    private UserService userService;

    @Resource
    private MfaTicketRepository mfaTicketRepository;

    @Resource
    private MfaVerifyStrategyFactory mfaVerifyStrategyFactory;

    @Resource
    private WebauthnService webauthnService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private TokenService tokenService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private OneTimeTokenGenerationSuccessService oneTimeTokenGenerationSuccessService;

    @Resource
    private TokenAuthenticationFilter tokenAuthenticationFilter;

    @Resource
    private SecurityContextRepository redisSecurityContextRepository;

    @Resource
    private CaptchaFactory captchaFactory;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
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
                //记住我
                .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices()))
                //一次性令牌
                .oneTimeTokenLogin((ott) -> {
                    //生成一次性令牌的路径
                    ott.tokenGeneratingUrl("/ott/generate");
                    //登录处理路径
                    ott.loginProcessingUrl("/login/ott");
                    //禁用默认提交页面
                    ott.showDefaultSubmitPage(false);
                    //令牌生成以及存储
                    ott.tokenService(oneTimeTokenService());
                    //令牌生成成功处理器
                    ott.tokenGenerationSuccessHandler(oneTimeTokenGenerationSuccessService);
                })
                .securityContext(securityContext -> {
                    securityContext.securityContextRepository(redisSecurityContextRepository);
                })
                .addFilterBefore(tokenAuthenticationFilter, SecurityContextHolderFilter.class)
                //记住我过滤器
                .addFilterBefore(rememberMeFilter(authenticationManager(http), rememberMeServices()), UsernamePasswordAuthenticationFilter.class)
                .logout(logout ->
                        logout
                                .logoutUrl("/logout")
                                .logoutSuccessHandler(logoutSuccessHandler())
                                .permitAll()
                );


        return http.build();
    }

    //身份认证管理器
    @Bean
    @Primary
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {

        return http.getSharedObject(AuthenticationManagerBuilder.class)
                //用户名密码身份认证
                .authenticationProvider(daoAuthenticationProvider())
                // 验证码认证
                .authenticationProvider(captchaAuthenticationProvider())
                //用于使用三方登录的身份认证
                .authenticationProvider(thirdPartyAuthenticationProvider())
                // 刷新令牌
                .authenticationProvider(refreshAuthenticationProvider())
                //记住我身份认证
                .authenticationProvider(rememberMeAuthenticationProvider())
                //一次性令牌认证
                .authenticationProvider(oneTimeTokenAuthenticationProvider())
                //webauthn通行密钥认证
                .authenticationProvider(webauthnAuthenticationProvider())
                // mfa双因子认证
                .authenticationProvider(mfaAuthenticationProvider())
                .parentAuthenticationManager(null)
                .build();
    }

    //基于用户名密码认证
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        // 密码防暴力破解登录
        LoginAttemptHandler loginAttemptService = new LoginAttemptHandler(redisHelper);
        DaoAuthenticationProvider authProvider = new UsernamePasswordAuthenticationProvider(userService, loginAttemptService);
        // 设置密码编辑器
        authProvider.setPasswordEncoder(passwordEncoder);
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public RememberMeAuthenticationProvider rememberMeAuthenticationProvider() {
        String secretKey = securityProperties.getRememberMe().getSecretKey();
        return new RememberMeAuthenticationProvider(secretKey);
    }

    //记住我
    @Bean
    public RememberMeAuthenticationFilter rememberMeFilter(AuthenticationManager authenticationManager, RememberMeServices rememberMeServices) {

        return new RememberMeAuthenticationFilter(authenticationManager, rememberMeServices);
    }

    @Bean
    public RememberMeServices rememberMeServices() {
        String secretKey = securityProperties.getRememberMe().getSecretKey();
        return new HeaderBasedRememberMeServices(secretKey, tokenService, userService);
    }

    // 验证码认证
    @Bean
    public CaptchaAuthenticationProvider captchaAuthenticationProvider() {
        return new CaptchaAuthenticationProvider(userService, captchaFactory);
    }

    //一次性令牌
    //使用数据库存储
    @Bean
    public OneTimeTokenService oneTimeTokenService() {
        return new JdbcOneTimeTokenService(jdbcTemplate);
    }

    @Bean
    public OneTimeTokenAuthenticationProvider oneTimeTokenAuthenticationProvider() {
        return new OneTimeTokenAuthenticationProvider(oneTimeTokenService(), userService);
    }

    // 通行密钥
    @Bean
    public PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository() {
        return new AtlasPublicKeyCredentialUserEntityRepository(userService);
    }

    @Bean
    public WebauthnAuthenticationProvider webauthnAuthenticationProvider(){

        return new WebauthnAuthenticationProvider(webauthnService,userService);
    }

    //三方登录认证
    @Bean
    public ThirdPartyAuthenticationProvider thirdPartyAuthenticationProvider() {

        return new ThirdPartyAuthenticationProvider(userService);
    }

    @Bean
    public RefreshAuthenticationProvider refreshAuthenticationProvider() {

        return new RefreshAuthenticationProvider(userService, tokenService);
    }

    @Bean
    public MfaAuthenticationProvider mfaAuthenticationProvider(){

        return new MfaAuthenticationProvider(
                userService,
                mfaTicketRepository,
                mfaVerifyStrategyFactory
        );
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {

        return logoutService;
    }

}
