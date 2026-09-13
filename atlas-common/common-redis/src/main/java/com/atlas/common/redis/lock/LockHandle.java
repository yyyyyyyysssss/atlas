package com.atlas.common.redis.lock;

public interface LockHandle extends AutoCloseable{

    boolean acquired();

    @Override
    void close();

}
