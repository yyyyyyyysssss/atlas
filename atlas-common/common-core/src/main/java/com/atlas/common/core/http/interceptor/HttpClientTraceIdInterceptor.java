package com.atlas.common.core.http.interceptor;

import com.atlas.common.core.constant.CommonConstant;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.MDC;

public class HttpClientTraceIdInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) {
        String tranceId = MDC.get(CommonConstant.TRACE_ID);
        if (tranceId != null) {
            httpRequest.addHeader(CommonConstant.TRACE_ID, tranceId);
        }
    }

}
