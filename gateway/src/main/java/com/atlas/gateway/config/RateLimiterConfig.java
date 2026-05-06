package com.atlas.gateway.config;

import com.atlas.common.core.response.IErrorCode;
import com.atlas.common.core.response.ResultGenerator;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
@EnableConfigurationProperties(RateLimitProperties.class)
@Slf4j
public class RateLimiterConfig {

    @Value("${spring.application.name:atlas}")
    private String applicationName;

    // Spring 会自动收集所有 KeyResolver 类型的 Bean
    private final Map<String, Function<ServerRequest, String>> keyResolvers;

    public RateLimiterConfig(Map<String, Function<ServerRequest, String>> keyResolvers) {
        this.keyResolvers = keyResolvers;
    }

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
    public RouterFunction<ServerResponse> routerFunctionsRateLimited(RateLimitProperties rateLimitProperties) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        final Function<ServerRequest, String> defaultIpResolver = keyResolvers.get("ipKeyResolver");
        for (RateLimitProperties.ServiceConfig service : rateLimitProperties.getServices()) {
            for (RateLimitProperties.LimitRule rule : service.getLimits()) {
                // 获取限流 Key 解析器 (优先使用配置的，否则默认按 IP)
                Function<ServerRequest, String> resolver;
                String resolverName = rule.getKeyResolverName() != null ? rule.getKeyResolverName() : "ipKeyResolver";
                if ("paramKeyResolver".equals(resolverName) && rule.getKeyName() != null) {
                    String targetParam = rule.getKeyName();
                    resolver = request -> request.param(targetParam)
                            .filter(val -> !val.isEmpty()) // 过滤掉空字符串
                            .orElseGet(() -> defaultIpResolver.apply(request));
                } else {
                    resolver = keyResolvers.getOrDefault(resolverName, defaultIpResolver);
                }
                // 构建路径
                RequestPredicate predicate = RequestPredicates.path(rule.getPath());
                // 构建方法
                if (rule.getMethods() != null && !rule.getMethods().isEmpty()) {
                    RequestPredicate methodPredicate = rule.getMethods().stream()
                            .map(m -> RequestPredicates.method(HttpMethod.valueOf(m.toUpperCase())))
                            .reduce(RequestPredicate::or)
                            .orElse(RequestPredicates.all()); // 兜底
                    predicate = predicate.and(methodPredicate);
                }
                // 生成唯一的 RouteId (将路径斜杠替换为下划线，防止重名)
                String routeId = service.getServiceId() + "_" + rule.getPath().replaceAll("/", "_");
                // 注册路由并挂载过滤器
                builder.add(
                        route(routeId)
                                .route(predicate, http())
                                .before(uri(service.getUri()))
                                .filter(stripPrefix(service.getStripPrefix()))
                                .filter((request, next) -> {
                                    HandlerFilterFunction<ServerResponse, ServerResponse> rateLimiterFilter =
                                            rateLimit(c -> c
                                                    .setCapacity(rule.getCapacity())
                                                    .setPeriod(Duration.ofSeconds(rule.getPeriodInSeconds()))
                                                    .setKeyResolver(resolver)
                                            );
                                    ServerResponse response = rateLimiterFilter.filter(request, next);
                                    // 判断是否触发限流
                                    if (response.statusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                                        log.warn("Rate limit triggered for path [{}], key [{}]", request.path(), resolver.apply(request));
                                        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .body(ResultGenerator.failed(new IErrorCode() {
                                                            @Override
                                                            public int getCode() {
                                                                return HttpStatus.TOO_MANY_REQUESTS.value();
                                                            }

                                                            @Override
                                                            public String getMessage() {
                                                                return HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase();
                                                            }
                                                        })
                                                );
                                    }
                                    return response;
                                })
                                .build()
                );
            }
        }
        return builder.build();
    }

    @Bean
    public HandlerFilterFunction<ServerResponse, ServerResponse> globalRateLimitFilter(RateLimitProperties rateLimitProperties, Map<String, Function<ServerRequest, String>> keyResolvers) {

        return (request, next) -> {
            // 1. 获取当前路由 ID
            String routeId = request.attribute(MvcUtils.GATEWAY_ROUTE_ID_ATTR)
                    .map(Object::toString).orElse(null);

            if (routeId == null) {
                return next.handle(request);
            }

            // 根据 routeId 匹配配置
            var serviceConfig = rateLimitProperties.getServices().stream()
                    .filter(s -> s.getServiceId().equals(routeId))
                    .findFirst()
                    .orElse(null);

            if (serviceConfig != null) {
                // 匹配具体接口规则
                var rule = serviceConfig.getLimits().stream()
                        .filter(r -> request.path().equals(r.getPath()))
                        .filter(r -> r.getMethods() == null || r.getMethods().contains(request.method().name()))
                        .findFirst()
                        .orElse(null);

                if (rule != null) {
                    // 执行限流逻辑
                    return executeRateLimit(request, next, rule, keyResolvers);
                }
            }

            return next.handle(request);
        };
    }

    /**
     * 将 Filter 注册到所有路由
     */
    @Bean
    public RouterFunction<ServerResponse> globalFilterRouter(
            HandlerFilterFunction<ServerResponse, ServerResponse> globalRateLimitFilter) {
        return RouterFunctions.route()
                .route(RequestPredicates.all(), request -> null) // 定义一个全匹配谓词，但 Handler 设为 null，因为我们主要用 filter
                .filter(globalRateLimitFilter)
                .build();
    }

    private ServerResponse executeRateLimit(
            ServerRequest request,
            HandlerFunction<ServerResponse> next,
            RateLimitProperties.LimitRule rule,
            Map<String, Function<ServerRequest, String>> keyResolvers) throws Exception {

        // 获取解析器 (默认 IP)
        String resolverName = rule.getKeyResolverName() != null ? rule.getKeyResolverName() : "pathKeyResolver";
        Function<ServerRequest, String> resolver = keyResolvers.getOrDefault(resolverName, keyResolvers.get("pathKeyResolver"));

        // 利用原生 Bucket4j 过滤器
        HandlerFilterFunction<ServerResponse, ServerResponse> rateLimiterFilter =
                rateLimit(c -> c
                        .setCapacity(rule.getCapacity())
                        .setPeriod(Duration.ofSeconds(rule.getPeriodInSeconds()))
                        .setKeyResolver(resolver)
                );
        ServerResponse response = rateLimiterFilter.filter(request, next);
        // 判断是否触发限流
        if (response.statusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            log.warn("Rate limit triggered for path [{}], key [{}]", request.path(), resolver.apply(request));
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ResultGenerator.failed(new IErrorCode() {
                                @Override
                                public int getCode() {
                                    return HttpStatus.TOO_MANY_REQUESTS.value();
                                }

                                @Override
                                public String getMessage() {
                                    return HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase();
                                }
                            })
                    );
        }
        return response;
    }

}
