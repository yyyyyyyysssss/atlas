package com.atlas.common.core.http.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "http.config")
public class HttpClientProperties {

    private long connectionRequestTimeout;

    private long responseTimeout;

    private int maxConnTotal;

    private int maxConnPerRoute;

    private boolean insecureSkipVerify;

    private String proxy;

}
