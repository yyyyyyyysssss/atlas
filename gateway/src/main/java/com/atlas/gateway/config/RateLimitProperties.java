package com.atlas.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/19 9:51
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private List<ServiceConfig> services = new ArrayList<>();

    @Getter
    @Setter
    public static class ServiceConfig {
        /**
         * 服务 ID（如 auth-service）
         */
        private String serviceId;

        /**
         * 转发目标基础 URI
         */
        private String uri;

        /**
         * 剥离前缀数量，默认 2
         */
        private int stripPrefix = 2;

        /**
         * 该服务下具体的限流规则列表
         */
        private List<LimitRule> limits = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class LimitRule {
        /**
         * 匹配路径
         */
        private String path;

        /**
         * 令牌桶容量
         */
        private int capacity;

        /**
         * 限流周期（秒）
         */
        private int periodInSeconds;

        /**
         * 请求方式限制
         */
        private List<String> methods;

        /**
         * 唯一标识解析器 Bean 名称，默认按 IP
         */
        private String keyResolverName = "ipKeyResolver";

        /**
         * 限流的具体参数名（配合 paramKeyResolver 使用）
         */
        private String keyName;
    }

}
