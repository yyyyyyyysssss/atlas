package com.atlas.common.core.web.filter;

import com.atlas.common.core.constant.CommonConstant;
import com.atlas.common.core.web.wrapper.HeaderEnhanceRequestWrapper;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * @Description
 * @Author ys
 * @Date 2025/2/8 23:27
 */

public class MDCTraceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,@Nonnull FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        // 注入响应头
        response.setHeader(CommonConstant.TRACE_ID, traceId);
        HeaderEnhanceRequestWrapper wrappedRequest = new HeaderEnhanceRequestWrapper(request);
        wrappedRequest.addHeader(CommonConstant.TRACE_ID, traceId);
        try {
            // 开启 RequestContextHolder 的子线程继承
            ServletRequestAttributes attributes = new ServletRequestAttributes(wrappedRequest, response);
            RequestContextHolder.setRequestAttributes(attributes, true);
            // 设置 MDC
            attributes.setAttribute(CommonConstant.TRACE_ID,traceId, RequestAttributes.SCOPE_REQUEST);
            MDC.put(CommonConstant.TRACE_ID,traceId);

            filterChain.doFilter(wrappedRequest,response);
        }finally {
            MDC.clear();
            RequestContextHolder.resetRequestAttributes();
        }

    }

    private String resolveTraceId(HttpServletRequest request){
        String traceId;
        if ((traceId=request.getHeader(CommonConstant.TRACE_ID)) == null){
            traceId=UUID.randomUUID().toString().replaceAll("-","");
        }
        return traceId;
    }

}
