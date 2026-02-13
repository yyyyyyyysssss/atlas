package com.atlas.common.core.api.feign;

import com.atlas.common.core.constant.CommonConstant;
import feign.Client;
import feign.Request;
import feign.Response;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;

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
        String traceId = MDC.get(CommonConstant.TRACE_ID);
        // 如果 traceId 为空，说明发生了线程切换
        if (traceId == null || traceId.isEmpty()) {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            traceId = (String) requestAttributes.getAttribute(CommonConstant.TRACE_ID, RequestAttributes.SCOPE_REQUEST);
            MDC.put(CommonConstant.TRACE_ID, traceId);
        }
        try {
            return delegate.execute(request, options);
        }finally {
            MDC.remove(CommonConstant.TRACE_ID);
        }
    }
}
