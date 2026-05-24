package com.atlas.common.core.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ServletHolder {

    private ServletHolder() {
        // 防止实例化
    }

    /**
     * 明确抓取当前线程的 HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    /**
     * 明确抓取当前线程的 HttpServletResponse
     */
    public static HttpServletResponse getResponse() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getResponse();
        }
        return null;
    }

}
