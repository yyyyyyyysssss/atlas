package com.atlas.common.core.api.feign.interceptor;

import com.atlas.common.core.constant.CommonConstant;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/12 17:52
 */
public class FeignTraceIdInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String traceId = MDC.get(CommonConstant.TRACE_ID);
        if(StringUtils.isNotEmpty(traceId)){
            requestTemplate.header(CommonConstant.TRACE_ID,traceId);
        }

    }
}
