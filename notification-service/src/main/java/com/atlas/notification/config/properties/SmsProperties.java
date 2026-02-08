package com.atlas.notification.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/5 15:40
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /**
     * 阿里云专用配置
     */
    private AliyunConfig aliyun;

    @Data
    public static class AliyunConfig {
        private String accessKey;
        private String accessSecret;
        private String endpoint;
        private String signature;
        private int timeout;

        // 业务编码与外部模板 ID 的映射
        private Map<String, String> templates;
    }

}
