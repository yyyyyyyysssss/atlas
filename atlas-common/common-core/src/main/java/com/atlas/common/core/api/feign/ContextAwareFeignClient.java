package com.atlas.common.core.api.feign;

import com.atlas.common.core.constant.CommonConstant;
import feign.Client;
import feign.Request;
import feign.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.util.Collection;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/13 9:32
 */
public class ContextAwareFeignClient implements Client {

    private final Client delegate;

    public ContextAwareFeignClient(Client delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        String originalTraceId = MDC.get(CommonConstant.TRACE_ID);
        if (StringUtils.isEmpty(originalTraceId)) {
            Collection<String> traceIdList = request.headers().get(CommonConstant.TRACE_ID);
            if (traceIdList != null && !traceIdList.isEmpty()) {
                MDC.put(CommonConstant.TRACE_ID, traceIdList.iterator().next());
            }
        }
        MDC.put(CommonConstant.THREAD_TYPE, Thread.currentThread().isVirtual() ? "V" : "P");
        try {
            return delegate.execute(request, options);
        } finally {
            if (StringUtils.isEmpty(originalTraceId)) {
                MDC.remove(CommonConstant.TRACE_ID);
            }
            MDC.remove(CommonConstant.THREAD_TYPE);
        }
    }
}
