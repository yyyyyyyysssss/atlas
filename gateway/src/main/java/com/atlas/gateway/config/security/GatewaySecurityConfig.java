package com.atlas.gateway.config.security;

import com.atlas.gateway.config.security.authentication.apikey.ApikeyAuthenticationProvider;
import com.atlas.gateway.config.security.authentication.apikey.SeparatorAntPathRequestMatcher;
import com.atlas.gateway.config.security.authorization.RequestPathAuthorizationManager;
import com.atlas.gateway.config.security.filter.ApikeyAuthenticationFilter;
import com.atlas.gateway.config.security.filter.FileCookieAuthenticationFilter;
import com.atlas.gateway.config.security.filter.GatewayHeaderHeaderPropagationFilter;
import com.atlas.security.filter.TokenAuthenticationFilter;
import com.atlas.security.handler.ForbiddenAccessHandler;
import com.atlas.security.handler.UnauthorizedEntryPoint;
import com.atlas.security.oauth2.OAuth2BearerTokenResolver;
import com.atlas.security.properties.SecurityProperties;
import com.atlas.security.resolver.NormalBearerTokenResolver;
import com.atlas.security.service.TokenService;
import jakarta.annotation.Resource;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 9:22
 */
@EnableWebSecurity
@Configuration
public class GatewaySecurityConfig {

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private TokenService tokenService;

    @Resource
    private NormalBearerTokenResolver normalBearerTokenResolver;

    @Resource
    private SecurityContextRepository redisSecurityContextRepository;

    @Resource
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Resource
    private OAuth2BearerTokenResolver oAuth2BearerTokenResolver;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                //跨域支持
                .cors(cors -> cors.configurationSource(cs -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    configuration.setAllowedOrigins(List.of("*"));
                    configuration.setAllowedMethods(List.of("*"));
                    configuration.setAllowedHeaders(List.of("*"));
                    return configuration;
                }))
                .anonymous(Customizer.withDefaults())
                .exceptionHandling(exception -> {
                    exception.authenticationEntryPoint(new UnauthorizedEntryPoint());
                    exception.accessDeniedHandler(new ForbiddenAccessHandler());
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //身份认证信息存储
                .securityContext(securityContext -> {
                    securityContext.securityContextRepository(redisSecurityContextRepository);
                })
                .authorizeHttpRequests(authorize -> {
                    // 放行路径
                    authorize.requestMatchers(securityProperties.getAuthorize().getPermit().toArray(new String[0])).permitAll();
                    //允许所有异步请求
                    authorize.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll();
                    // 只需要通过身份认证就能访问的路径
                    authorize.requestMatchers(securityProperties.getAuthorize().getAuthenticated().toArray(new String[0])).authenticated();
                    // 基于请求头apikey授权
                    authorize.requestMatchers(securityProperties.getAuthorize().requestHeadAuthenticationPath()).hasAuthority(ApikeyAuthenticationProvider.APIKEY_ROLE_CODE);
                    // 基于oauth2 scope授权
                    List<SecurityProperties.ResourceAuthenticationConfig> resourceRules = securityProperties.getAuthorize().getResourceAuthorizations();
                    if (resourceRules != null) {
                        for (SecurityProperties.ResourceAuthenticationConfig rule : resourceRules) {
                            // 这里动态绑定 pattern 和 scope
                            authorize.requestMatchers(rule.getPattern())
                                    .hasAuthority(rule.getScope());
                        }
                    }
                    // 兜底策略
                    authorize.anyRequest().access(requestPathAuthorizationManager());
                })
                // 该过滤器解析token并校验通过后由SecurityContextHolderFilter过滤器加载SecurityContext
                .addFilterBefore(tokenAuthenticationFilter(), SecurityContextHolderFilter.class)
                // 用于文件访问的过滤器
                .addFilterBefore(fileCookieAuthenticationFilter(), SecurityContextHolderFilter.class)
                // 基于请求头apikey认证的过滤器
                .addFilterBefore(apikeyAuthenticationFilter(authenticationManager(http)), HeaderWriterFilter.class)
                // 设置用户信息到请求头
                .addFilterAfter(gatewayHeaderHeaderPropagationFilter(), AuthenticationFilter.class)
                // oauth2资源服务器
                .oauth2ResourceServer((resourceServer) -> {
                    resourceServer.jwt(jwt -> {
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter);
                    });
                    resourceServer.bearerTokenResolver(oAuth2BearerTokenResolver);
                });

        return http.build();
    }

    //身份认证管理器
    @Bean
    @Primary
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                //基于apikey认证
                .authenticationProvider(apikeyAuthenticationProvider())
                .parentAuthenticationManager(null)
                .build();
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter(){

        return new TokenAuthenticationFilter(tokenService, normalBearerTokenResolver);
    }

    @Bean
    public FileCookieAuthenticationFilter fileCookieAuthenticationFilter() {

        return new FileCookieAuthenticationFilter(tokenService);
    }

    @Bean
    public GatewayHeaderHeaderPropagationFilter gatewayHeaderHeaderPropagationFilter(){

        return new GatewayHeaderHeaderPropagationFilter();
    }

    //基于请求路径的权限管理器
    @Bean
    public RequestPathAuthorizationManager requestPathAuthorizationManager() {

        return new RequestPathAuthorizationManager();
    }

    //基于请求头apikey的认证过滤器
    @Bean
    public RequestHeaderAuthenticationFilter apikeyAuthenticationFilter(AuthenticationManager authenticationManager) {
        String[] antPaths = securityProperties.getAuthorize().requestHeadAuthenticationPath();
        RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter = new ApikeyAuthenticationFilter();
        requestHeaderAuthenticationFilter.setExceptionIfHeaderMissing(false);
        requestHeaderAuthenticationFilter.setRequiresAuthenticationRequestMatcher(new SeparatorAntPathRequestMatcher(antPaths));
        requestHeaderAuthenticationFilter.setAuthenticationManager(authenticationManager);
        return requestHeaderAuthenticationFilter;
    }

    @Bean
    public ApikeyAuthenticationProvider apikeyAuthenticationProvider() {

        return new ApikeyAuthenticationProvider(securityProperties.getAuthorize().getRequestHeadAuthentications());
    }

}
