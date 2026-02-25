package com.atlas.gateway.config.security.filter;

import com.atlas.common.core.constant.CommonConstant;
import com.atlas.common.core.web.wrapper.HeaderEnhanceRequestWrapper;
import com.atlas.security.model.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/25 15:29
 */
public class GatewayHeaderHeaderPropagationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 必须已认证且不是匿名用户
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof SecurityUser securityUser) {
            HeaderEnhanceRequestWrapper wrappedRequest = new HeaderEnhanceRequestWrapper(request);
            wrappedRequest.addHeader(CommonConstant.USER_ID,securityUser.getId().toString());
            wrappedRequest.addHeader(CommonConstant.USER_FULL_NAME,securityUser.getFullName());

            filterChain.doFilter(wrappedRequest,response);
        }else {
            filterChain.doFilter(request,response);
        }
    }

}
