package com.atlas.common.core.http.interceptor;

import com.atlas.common.core.constant.CommonConstant;
import com.atlas.common.core.context.UserContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HttpClientUserContextInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
        UserContext.UserObject user = UserContext.getUser();
        if(user != null){
            if (user.getUserId() != null) {
                httpRequest.addHeader(CommonConstant.USER_ID, user.getUserId());
            }
            if (user.getFullName() != null) {
                String encodedName = URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8);
                httpRequest.addHeader(CommonConstant.USER_FULL_NAME, encodedName);
            }
        }
    }
}
