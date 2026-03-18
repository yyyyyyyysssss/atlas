package com.atlas.gateway.config;

import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions.rateLimit;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/18 16:12
 */
@Configuration
public class RateLimiterConfig {

    @Value("${spring.application.name:atlas}")
    private String applicationName;

    @Bean
    public AsyncProxyManager<String> redisAsyncProxyManager(RedisConnectionFactory connectionFactory) {
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
        RedisClient redisClient = (RedisClient) lettuceFactory.getNativeClient();
        String redisKeyPrefix = applicationName + ":rate-limit:";
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(new PrefixedStringCodec(redisKeyPrefix));
        return LettuceBasedProxyManager
                .builderFor(connection)
                .build()
                .asAsync();
    }

    @Bean
    public RouterFunction<ServerResponse> routerFunctionsRateLimited() {
        return route("auth_email_limit")
                .POST("/api/auth/code/send-email", http())
                .before(uri("http://localhost:9096"))
                .filter(stripPrefix(2))
                .filter(
                        rateLimit(c -> c.setCapacity(1)
                                .setPeriod(Duration.ofMinutes(1))
                                .setKeyResolver(request -> {
                                            HttpServletRequest servletRequest = request.servletRequest();
                                            String ip = servletRequest.getHeader("X-Forwarded-For");
                                            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                                                ip = servletRequest.getRemoteAddr();
                                            }
                                            String rawIp = (ip != null && ip.contains(",")) ? ip.split(",")[0].trim() : ip;
                                            return (rawIp != null) ? rawIp.replace(":", ".") : "unknown";
                                        }
                                )
                        )
                )
                .build();
    }

}
