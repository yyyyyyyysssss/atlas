package com.atlas.common.security.filter;


import com.atlas.common.security.enums.TokenType;
import com.atlas.common.security.model.PayloadInfo;
import com.atlas.common.security.repository.RedisSecurityContextRepository;
import com.atlas.common.security.resolver.NormalBearerTokenResolver;
import com.atlas.common.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @Description OncePerRequestFilter 一次请求中只会执行一次的过滤器
 * @Author ys
 * @Date 2023/7/26 17:30
 */
@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final NormalBearerTokenResolver normalBearerTokenResolver;

    private final TokenService tokenService;

    public TokenAuthenticationFilter(TokenService tokenService, NormalBearerTokenResolver normalBearerTokenResolver) {
        this.tokenService = tokenService;
        this.normalBearerTokenResolver = normalBearerTokenResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //已授权的接口直接放行 或已设置tokenId的直接放行
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null
                && securityContext.getAuthentication().isAuthenticated()
                && !(securityContext.getAuthentication() instanceof AnonymousAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 已加载过tokenId的不再重复加载
        if (request.getAttribute(RedisSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME) != null) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = normalBearerTokenResolver.resolve(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            //设置请求属性  由RedisSecurityContextRepository加载SecurityContext
            PayloadInfo payloadInfo = tokenService.verify(token, TokenType.ACCESS_TOKEN);
            if (payloadInfo != null && payloadInfo.getId() != null) {
                request.setAttribute(RedisSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME, payloadInfo.getId());
            }
        } catch (AuthenticationException e) {
            log.debug("Token authentication failed: {}", e.getMessage());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            request.removeAttribute(RedisSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME);
        }

    }
}
