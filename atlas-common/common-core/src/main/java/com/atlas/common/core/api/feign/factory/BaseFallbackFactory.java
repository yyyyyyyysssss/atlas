package com.atlas.common.core.api.feign.factory;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.openfeign.FallbackFactory;

public interface BaseFallbackFactory<T> extends FallbackFactory<T> {

    @Override
    default T create(Throwable cause) {
        // 如果是熔断器开启，直接抛出，让 GlobalExceptionHandler 返回 503
        if (cause instanceof CallNotPermittedException) {
            throw (CallNotPermittedException) cause;
        }
        return createFallback(cause);
    }

    T createFallback(Throwable cause);

}
