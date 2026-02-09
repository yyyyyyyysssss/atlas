package com.atlas.notification.config.thread;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/9 11:23
 */
@Data
@Component
@ConfigurationProperties(prefix = "thread.notification")
public class NotificationThreadProperties {

    private int coreSize = 10;

    private int maxSize = 50;

    private int queueCapacity = 1000;

}
