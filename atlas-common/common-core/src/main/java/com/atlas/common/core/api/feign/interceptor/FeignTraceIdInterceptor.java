package com.atlas.common.core.api.feign.interceptor;

import com.atlas.common.core.constant.CommonConstant;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/12 17:52
 */
public class FeignTraceIdInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String traceId = MDC.get(CommonConstant.TRACE_ID);
        if(StringUtils.isEmpty(traceId)){
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            traceId = (String) requestAttributes.getAttribute(CommonConstant.TRACE_ID, RequestAttributes.SCOPE_REQUEST);
            MDC.put(CommonConstant.TRACE_ID, traceId);
        }
        if(!traceId.isEmpty()){
            requestTemplate.header(CommonConstant.TRACE_ID,traceId);
        }

    }
}
