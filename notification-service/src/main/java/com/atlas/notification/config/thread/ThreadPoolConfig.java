package com.atlas.notification.config.thread;

import com.atlas.common.core.thread.ThreadPoolFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/9 11:14
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {


    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor notificationExecutor(NotificationThreadProperties notificationThreadProperties) {
        log.info("[ThreadPool-Config] Initializing notificationExecutor: core={}, max={}, queue={}",
                notificationThreadProperties.getCoreSize(),
                notificationThreadProperties.getMaxSize(),
                notificationThreadProperties.getQueueCapacity());

        return ThreadPoolFactory
                .builder("notification-")
                .virtual()
                .build();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setThreadNamePrefix("taskScheduler-virtual-");
        // 核心：绑定 Java 21 的虚拟线程执行器
        scheduler.setVirtualThreads(true);
        return scheduler;
    }

}
