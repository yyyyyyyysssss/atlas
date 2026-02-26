package com.atlas.file.config.thread;

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


    @Bean("defaultThreadPool")
    public ThreadPoolTaskExecutor defaultThreadPool(FileThreadProperties fileThreadProperties) {
        log.info("[ThreadPool-Config] Initializing defaultThreadPool: core={}, max={}, queue={}",
                fileThreadProperties.getCoreSize(),
                fileThreadProperties.getMaxSize(),
                fileThreadProperties.getQueueCapacity());

        return ThreadPoolFactory
                .builder("defaultThreadPool-")
                .coreSize(fileThreadProperties.getCoreSize())
                .maxSize(fileThreadProperties.getMaxSize())
                .queueCapacity(fileThreadProperties.getQueueCapacity())
                .build();
    }

}
