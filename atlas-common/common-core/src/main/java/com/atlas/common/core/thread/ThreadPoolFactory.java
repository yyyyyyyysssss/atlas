package com.atlas.common.core.thread;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolFactory {

    public static ThreadPoolTaskExecutor create(String namePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 基础识别配置
        executor.setThreadNamePrefix(namePrefix);

        // 链路追踪核心：注入 MDC 装饰器
        executor.setTaskDecorator(new MDCTaskDecorator());

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
        // 这里不直接调用 initialize()，留给各自服务设置完 CoreSize/MaxSize 后手动调用
        return executor;
    }

}
