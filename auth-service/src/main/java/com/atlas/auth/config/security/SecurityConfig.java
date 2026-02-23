package com.atlas.auth.config.security;


import com.atlas.auth.config.security.authentication.provider.EmailAuthenticationProvider;
import com.atlas.auth.config.security.authentication.provider.ThirdPartyAuthenticationProvider;
import com.atlas.auth.config.security.filter.HeaderAuthenticationFilter;
import com.atlas.auth.config.security.handler.LoginAttemptHandler;
import com.atlas.auth.config.security.handler.MagicLinkOneTimeTokenGenerationSuccessHandler;
import com.atlas.auth.service.LogoutService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.handler.ForbiddenAccessHandler;
import com.atlas.security.handler.UnauthorizedEntryPoint;
import com.atlas.security.properties.SecurityProperties;
import com.atlas.security.repository.RedisSecurityContextRepository;
import com.atlas.security.resolver.NormalBearerTokenResolver;
import jakarta.annotation.Resource;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import java.nio.charset.StandardCharsets;

@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityConfig {

    @Resource
    private NormalBearerTokenResolver normalBearerTokenResolver;

    @Resource
    private LogoutService logoutService;

    @Resource
    private RedisHelper redisHelper;

    @Resource
    private UserDetailsService userService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private JdbcTemplate jdbcTemplate;

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
                            // 其余的只要通过认证即可 这里的“认证”是指你通过 Filter 从 Header 解析出的用户信息
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
                    ott.tokenGenerationSuccessHandler(new MagicLinkOneTimeTokenGenerationSuccessHandler(securityProperties.getLoginPage()));
                })
                .addFilterAfter(headerAuthenticationFilter(), SecurityContextHolderFilter.class)
                //记住我过滤器
                .addFilterBefore(rememberMeFilter(authenticationManager(http),rememberMeServices()), UsernamePasswordAuthenticationFilter.class)
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
                //邮箱验证码认证
                .authenticationProvider(emailAuthenticationProvider())
                //用于使用三方登录的身份认证
                .authenticationProvider(thirdPartyAuthenticationProvider())
                //记住我身份认证
                .authenticationProvider(rememberMeAuthenticationProvider())
                //一次性令牌认证
                .authenticationProvider(oneTimeTokenAuthenticationProvider())
                .parentAuthenticationManager(null)
                .build();
    }

    //基于用户名密码认证
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        // 密码防暴力破解登录
        LoginAttemptHandler loginAttemptService = new LoginAttemptHandler(redisHelper);
        DaoAuthenticationProvider authProvider = new UsernamePasswordAuthenticationProvider(userService,loginAttemptService);
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
        return new TokenBasedRememberMeServices(secretKey, userService, TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256);
    }

    //三方登录认证
    @Bean
    public EmailAuthenticationProvider emailAuthenticationProvider() {
        return new EmailAuthenticationProvider(userService, redisHelper);
    }

    //一次性令牌
    //使用数据库存储
    @Bean
    public OneTimeTokenService oneTimeTokenService() {
        return new JdbcOneTimeTokenService(jdbcTemplate);
    }

    @Bean
    public OneTimeTokenAuthenticationProvider oneTimeTokenAuthenticationProvider() {
        return new OneTimeTokenAuthenticationProvider(oneTimeTokenService(),userService);
    }

    //三方登录认证
    @Bean
    public ThirdPartyAuthenticationProvider thirdPartyAuthenticationProvider() {
        ThirdPartyAuthenticationProvider thirdPartyAuthenticationProvider = new ThirdPartyAuthenticationProvider();
        thirdPartyAuthenticationProvider.setUserDetailsService(userService);
        return thirdPartyAuthenticationProvider;
    }

    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter(){
        return new HeaderAuthenticationFilter();
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {

        return (request, response, authentication) -> {
            String tokenId = (String) request.getAttribute(RedisSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME);
            if (tokenId == null) {
                // 尝试手动解析
                tokenId = normalBearerTokenResolver.resolve(request);
            }
            if (tokenId != null) {
                try {
                    logoutService.logout(tokenId);
                }catch (Exception e){
                    log.error("Logout error for token: {}", tokenId, e);
                }

            }
            // 清理本地上下文
            SecurityContextHolder.clearContext();
            // 返回标准响应
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Content-type", MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
            Result<Object> result = ResultGenerator.ok();
            response.getWriter().println(JsonUtils.toJson(result));
            response.getWriter().flush();
        };
    }



}
