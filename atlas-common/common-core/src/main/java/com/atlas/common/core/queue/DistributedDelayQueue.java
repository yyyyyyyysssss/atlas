package com.atlas.common.core.queue;

import java.util.concurrent.TimeUnit;

public interface DistributedDelayQueue {


    <T> void sendMessage(String queueName, T data, long delay, TimeUnit unit);


}
