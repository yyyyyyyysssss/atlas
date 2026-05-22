package com.atlas.gateway.config;

import com.atlas.common.core.constant.CommonConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.application.name:atlas}")
    private String applicationName;



    @Bean("ipKeyResolver")
    public Function<ServerRequest, String> ipKeyResolver() {
        Function<ServerRequest, String> extractor = request -> {
            HttpServletRequest servletRequest = request.servletRequest();
            String ip = servletRequest.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = servletRequest.getRemoteAddr();
            }
            String rawIp = (ip != null && ip.contains(",")) ? ip.split(",")[0].trim() : ip;
            return (rawIp != null) ? rawIp.replace(":", ".") : "unknown";
        };
        return extractor.andThen(withPrefix());
    }

    @Bean("pathKeyResolver")
    public Function<ServerRequest, String> pathKeyResolver() {
        Function<ServerRequest, String> extractor = request -> {
            String path = request.path();
            return path.replace("/", "-");
        };
        return extractor.andThen(withPrefix());
    }

    @Bean("userKeyResolver")
    public Function<ServerRequest, String> userKeyResolver() {
        Function<ServerRequest, String> extractor = request -> {
            HttpServletRequest servletRequest = request.servletRequest();
            String userId = servletRequest.getHeader(CommonConstant.USER_ID);
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }
            // 如果退化成 IP，直接调用 ipKeyResolver()，它内部已经带有了前缀，因此这里直接返回
            return ipKeyResolver().apply(request);
        };

        return request -> {
            String res = extractor.apply(request);
            // 如果已经是拼好前缀的（走了解析 IP 分支），直接返回；否则拼接前缀
            return res.startsWith(applicationName + ":rate-limit:") ? res : withPrefix().apply(res);
        };
    }

    public Function<ServerRequest, String> createParamKeyResolver(String paramName) {
        Function<ServerRequest, String> extractor = request -> request.param(paramName)
                .filter(val -> !val.isEmpty()) // 过滤掉空字符串
                .orElse(null); // 如果参数拿不到，先返空，交由外层兜底成 IP

        return request -> {
            String rawParam = extractor.apply(request);
            if (rawParam != null) {
                // 成功拿到参数，走前缀包装逻辑
                return withPrefix().apply(rawParam);
            }
            // 如果请求中没带这个参数，则退化使用当前类的默认 IP 限流解析器（已自带前缀）
            return ipKeyResolver().apply(request);
        };
    }

    private Function<String, String> withPrefix() {
        return rawKey -> applicationName + ":rate-limit:" + rawKey;
    }

}
