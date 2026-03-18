package com.atlas.common.core.thread;

import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolFactory {

    public static Builder builder(String namePrefix) {

        return new Builder(namePrefix);
    }

    public static class Builder {

        private final ThreadPoolTaskExecutor executor;

        private boolean isVirtual = false;

        private Builder(String namePrefix) {
            this.executor = new ThreadPoolTaskExecutor();
            // 基础识别配置
            executor.setThreadNamePrefix(namePrefix);

            // 链路追踪核心：注入装饰器
            executor.setTaskDecorator(new CompositeTaskDecorator(
                    List.of(
                            new MDCTaskDecorator(),
                            new UserContextTaskDecorator()
                    ))
            );

            // 拒绝策略 当线程池满了且队列也满时，由提交任务的线程执行
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

            // 在 JVM 关闭时，等待当前任务执行完毕后再销毁
            executor.setWaitForTasksToCompleteOnShutdown(true);

            // 最多等待时间（秒），防止僵死任务导致容器无法正常停止
            executor.setAwaitTerminationSeconds(60);

            // 允许核心线程超时 也就是setKeepAliveSeconds也作用与核心线程
            executor.setAllowCoreThreadTimeOut(true);

            // 线程的最大空闲存活时间
            executor.setKeepAliveSeconds(300);
        }

        public Builder coreSize(int coreSize) {
            executor.setCorePoolSize(coreSize);
            return this;
        }

        public Builder maxSize(int maxSize) {
            executor.setMaxPoolSize(maxSize);
            return this;
        }

        public Builder queueCapacity(int capacity) {
            executor.setQueueCapacity(capacity);
            return this;
        }

        /**
         * 开启虚拟线程模式
         */
        public Builder virtual() {
            this.isVirtual = true;
            // 虚拟线程不需要“池”的概念，核心线程设为 0，最大值设为理论无穷
            executor.setCorePoolSize(0);
            executor.setMaxPoolSize(Integer.MAX_VALUE);
            // 虚拟线程通常不使用阻塞队列排队，直接创建新线程
            executor.setQueueCapacity(0);
            return this;
        }

        public ThreadPoolTaskExecutor build() {
            if (isVirtual) {
                // 使用虚拟线程工厂
                executor.setThreadFactory(Thread.ofVirtual()
                        .name(executor.getThreadNamePrefix(), 0)
                        .factory());
            }
            executor.initialize();
            return executor;
        }

    }

}
