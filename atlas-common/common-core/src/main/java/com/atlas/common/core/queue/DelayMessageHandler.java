package com.atlas.common.core.queue;

public interface DelayMessageHandler<T> {

    /**
     * 处理逻辑
     */
    void handle(T payload);

    /**
     * 该处理器关注的 Topic
     */
    String getTopic();

}
