package com.atlas.gateway.config;

import com.atlas.common.core.constant.CommonConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.function.Function;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/19 10:58
 */
@Configuration
public class KeyResolverConfiguration {


    @Bean("ipKeyResolver")
    public Function<ServerRequest, String> ipKeyResolver() {
        return request -> {
            HttpServletRequest servletRequest = request.servletRequest();
            String ip = servletRequest.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = servletRequest.getRemoteAddr();
            }
            String rawIp = (ip != null && ip.contains(",")) ? ip.split(",")[0].trim() : ip;
            return (rawIp != null) ? rawIp.replace(":", ".") : "unknown";
        };
    }

    @Bean("pathKeyResolver")
    public Function<ServerRequest, String> pathKeyResolver() {
        return request -> {
            String path = request.path();
            return path.replace("/", "-");
        };
    }

    @Bean("userKeyResolver")
    public Function<ServerRequest, String> userKeyResolver() {
        return request -> {
            HttpServletRequest servletRequest = request.servletRequest();
            String userId = servletRequest.getHeader(CommonConstant.USER_ID);
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }
            // 兜底：如果未登录，退化为按 IP 限流，防止匿名刷接口
            return ipKeyResolver().apply(request);
        };
    }

}
