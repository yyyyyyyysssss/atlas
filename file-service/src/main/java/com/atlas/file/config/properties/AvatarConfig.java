package com.atlas.file.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file.avatar")
public class AvatarConfig {

    private String baseUrl;

    private String defaultType;

    private String defaultFormat;

}
