package com.atlas.notification.config.thread;

import com.atlas.common.core.thread.ThreadPoolFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/9 11:14
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {


    @Bean("notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor(NotificationThreadProperties notificationThreadProperties) {
        log.info("[ThreadPool-Config] Initializing notificationExecutor: core={}, max={}, queue={}",
                notificationThreadProperties.getCoreSize(),
                notificationThreadProperties.getMaxSize(),
                notificationThreadProperties.getQueueCapacity());

        return ThreadPoolFactory
                .builder("notification-")
                .coreSize(notificationThreadProperties.getCoreSize())
                .maxSize(notificationThreadProperties.getMaxSize())
                .queueCapacity(notificationThreadProperties.getQueueCapacity())
                .build();
    }

}
