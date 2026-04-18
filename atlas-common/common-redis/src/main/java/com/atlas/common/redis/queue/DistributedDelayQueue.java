package com.atlas.common.redis.queue;

import java.util.concurrent.TimeUnit;

public interface DistributedDelayQueue {


    <T> void addJob(String queueName, T data, long delay, TimeUnit unit);


}
