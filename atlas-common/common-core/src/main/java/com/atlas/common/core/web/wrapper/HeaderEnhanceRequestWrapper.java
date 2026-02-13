package com.atlas.common.core.web.wrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/13 17:30
 */
public class HeaderEnhanceRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders;

    public HeaderEnhanceRequestWrapper(HttpServletRequest request) {
        super(request);
        this.customHeaders = new HashMap<>();
    }

    public HeaderEnhanceRequestWrapper addHeader(String name, String value) {
        if (value != null) {
            this.customHeaders.put(name, value);
        }
        return this;
    }

    @Override
    public String getHeader(String name) {
        String value = customHeaders.get(name);
        return (value != null) ? value : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>();
        Enumeration<String> originalNames = super.getHeaderNames();
        if (originalNames != null) {
            names.addAll(Collections.list(originalNames));
        }
        names.addAll(customHeaders.keySet());
        return Collections.enumeration(names);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String value = customHeaders.get(name);
        if (value != null) {
            return Collections.enumeration(Collections.singletonList(value));
        }
        return super.getHeaders(name);
    }
}
