package com.atlas.user.config.thread;

import com.atlas.common.core.thread.ThreadPoolFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/9 11:14
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {


    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        log.info("[ThreadPool-Config] Initializing taskExecutor");
        return ThreadPoolFactory
                .builder("taskExecutor-")
                .virtual()
                .build();
    }

}
