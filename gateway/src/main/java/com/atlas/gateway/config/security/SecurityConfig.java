package com.atlas.gateway.config.security;

import com.atlas.gateway.config.security.authentication.apikey.ApikeyAuthenticationProvider;
import com.atlas.gateway.config.security.authentication.apikey.SeparatorAntPathRequestMatcher;
import com.atlas.gateway.config.security.authorization.RequestPathAuthorizationManager;
import com.atlas.gateway.config.security.filter.FileCookieAuthenticationFilter;
import com.atlas.gateway.config.security.filter.TokenAuthenticationFilter;
import com.atlas.security.properties.SecurityProperties;
import com.atlas.security.service.TokenService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 9:22
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private TokenService tokenService;

    @Resource
    private SecurityContextRepository redisSecurityContextRepository;

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
                    //放行的路径
                    authorize
                            //允许所有人访问的路径
                            .requestMatchers(securityProperties.getAuthorize().getPermit().toArray(new String[0])).permitAll()
                            //允许所有异步请求
                            .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                            //只需要通过身份认证就能访问的路径
                            .requestMatchers(securityProperties.getAuthorize().getAuthenticated().toArray(new String[0])).authenticated()
                            //基于请求头apikey授权
                            .requestMatchers(securityProperties.requestHeadAuthenticationPath()).hasAuthority(ApikeyAuthenticationProvider.APIKEY_ROLE_CODE)
                            //必须校验权限的路径
                            .anyRequest().access(requestPathAuthorizationManager());
                })
                //该过滤器解析token并校验通过后由SecurityContextHolderFilter过滤器加载SecurityContext
                .addFilterBefore(tokenAuthenticationFilter(), SecurityContextHolderFilter.class)
                // 用于文件访问的过滤器
                .addFilterBefore(fileCookieAuthenticationFilter(), SecurityContextHolderFilter.class)
                //基于请求头apikey认证的过滤器
                .addFilterBefore(apikeyAuthenticationFilter(authenticationManager(http)), HeaderWriterFilter.class);

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

    //token过滤器
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {

        return new TokenAuthenticationFilter(tokenService);
    }

    @Bean
    public FileCookieAuthenticationFilter fileCookieAuthenticationFilter(){

        return new FileCookieAuthenticationFilter(tokenService);
    }

    //基于请求路径的权限管理器
    @Bean
    public RequestPathAuthorizationManager requestPathAuthorizationManager() {

        return new RequestPathAuthorizationManager(permissionCache());
    }

    //基于请求头apikey的认证过滤器
    @Bean
    public RequestHeaderAuthenticationFilter apikeyAuthenticationFilter(AuthenticationManager authenticationManager) {
        String[] antPaths = securityProperties.requestHeadAuthenticationPath();
        RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter = new RequestHeaderAuthenticationFilter();
        requestHeaderAuthenticationFilter.setPrincipalRequestHeader("apikey");
        requestHeaderAuthenticationFilter.setExceptionIfHeaderMissing(false);
        requestHeaderAuthenticationFilter.setRequiresAuthenticationRequestMatcher(new SeparatorAntPathRequestMatcher(antPaths));
        requestHeaderAuthenticationFilter.setAuthenticationManager(authenticationManager);
        return requestHeaderAuthenticationFilter;
    }
    @Bean
    public ApikeyAuthenticationProvider apikeyAuthenticationProvider() {

        return new ApikeyAuthenticationProvider(securityProperties.getRequestHeadAuthentications());
    }

    // 用于缓存权限校验的结果
    @Bean
    public Cache<String, Boolean> permissionCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(20000)
                .build();
    }

}
