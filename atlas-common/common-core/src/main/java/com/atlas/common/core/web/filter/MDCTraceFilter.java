package com.atlas.common.core.web.filter;

import com.atlas.common.core.constant.CommonConstant;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * @Description
 * @Author ys
 * @Date 2025/2/8 23:27
 */

public class MDCTraceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response,@Nonnull FilterChain filterChain) throws ServletException, IOException {
        String traceId;
        if ((traceId=request.getHeader(CommonConstant.TRACE_ID)) == null){
            traceId=UUID.randomUUID().toString().replaceAll("-","");
        }
        try {
            MDC.put(CommonConstant.TRACE_ID,traceId);
            filterChain.doFilter(request,response);
        }finally {
            MDC.clear();
        }

    }
}
