package com.atlas.file.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/17 16:49
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    private String accessUrl;

    /**
     * 头像配置
     */
    private Avatar avatar = new Avatar();

    @Getter
    @Setter
    public static class Avatar {

        /**
         * 头像服务基础地址
         */
        private String baseUrl;

        /**
         * 默认头像类型
         */
        private String defaultType;

        /**
         * 默认格式
         */
        private String defaultFormat;
    }
}
