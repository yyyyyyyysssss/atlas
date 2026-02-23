package com.atlas.common.core.web.filter;

import com.atlas.common.core.constant.CommonConstant;
import com.atlas.common.core.context.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(CommonConstant.USER_ID);
        String fullName = request.getHeader(CommonConstant.USER_FULL_NAME);
        try {
            if(StringUtils.isNumeric(userId)){
                String decodedName = fullName;
                if (StringUtils.isNotEmpty(fullName)) {
                    try {
                        decodedName = URLDecoder.decode(fullName, StandardCharsets.UTF_8);
                    }catch (Exception e){
                        logger.warn("UserContextFilter: fullName 解码失败: " + fullName);
                    }
                }
                UserContext.setUser(Long.valueOf(userId),decodedName);
            }
            filterChain.doFilter(request, response);
        }finally {
            UserContext.clear();
        }
    }
}
