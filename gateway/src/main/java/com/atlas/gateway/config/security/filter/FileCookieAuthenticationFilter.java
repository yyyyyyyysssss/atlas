package com.atlas.gateway.config.security.filter;

import com.atlas.security.repository.RedisSecurityContextRepository;
import com.atlas.security.enums.TokenType;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @Description OncePerRequestFilter 一次请求中只会执行一次的过滤器
 * @Author ys
 * @Date 2023/7/26 17:30
 */
public class FileCookieAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private final RequestMatcher tokenEndpointMatcher;

    public FileCookieAuthenticationFilter(TokenService tokenService){
        this.tokenService = tokenService;
        this.tokenEndpointMatcher = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET,"/file/**");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(!this.tokenEndpointMatcher.matches(request)){
            filterChain.doFilter(request, response);
            return;
        }
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null){
            filterChain.doFilter(request,response);
            return;
        }
        String token = extractToken(request);
        if(token != null){
            PayloadInfo payloadInfo = tokenService.verify(token);
            if (payloadInfo != null){
                String tokenId = payloadInfo.getId();
                request.setAttribute(RedisSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME, tokenId);
            }
        }
        filterChain.doFilter(request,response);
    }

    private String extractToken(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equalsIgnoreCase(cookie.getName()) || "access_token".equalsIgnoreCase(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String accessToken = request.getParameter("accessToken");
        if(accessToken == null){
            accessToken = request.getParameter("access_token");
        }
        return accessToken;
    }
}
