package com.atlas.gateway.config.security.filter;


import com.atlas.gateway.config.security.NormalBearerTokenResolver;
import com.atlas.security.repository.RedisSecurityContextRepository;
import com.atlas.security.enums.TokenType;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @Description OncePerRequestFilter 一次请求中只会执行一次的过滤器
 * @Author ys
 * @Date 2023/7/26 17:30
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final NormalBearerTokenResolver normalBearerTokenResolver;

    private final TokenService tokenService;

    public TokenAuthenticationFilter(TokenService tokenService){
        this.tokenService = tokenService;
        normalBearerTokenResolver = new NormalBearerTokenResolver();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //已授权的接口直接放行
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null && securityContext.getAuthentication().isAuthenticated()){
            filterChain.doFilter(request,response);
            return;
        }
        String token = normalBearerTokenResolver.resolve(request);
        if (token == null){
            filterChain.doFilter(request, response);
            return;
        }
        //设置请求属性  由RedisSecurityContextRepository加载SecurityContext
        PayloadInfo payloadInfo = tokenService.verify(token);
        if (payloadInfo != null){
            String tokenId = payloadInfo.getId();
            request.setAttribute(RedisSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME, tokenId);
        }
        filterChain.doFilter(request,response);
    }
}
