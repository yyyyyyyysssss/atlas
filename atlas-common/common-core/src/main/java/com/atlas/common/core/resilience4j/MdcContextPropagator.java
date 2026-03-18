package com.atlas.common.core.resilience4j;

import io.github.resilience4j.core.ContextPropagator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/12 15:36
 */
@Slf4j
@Component
public class MdcContextPropagator implements ContextPropagator<Map<String, String>> {
    @Override
    public Supplier<Optional<Map<String, String>>> retrieve() {

        return () -> Optional.ofNullable(MDC.getCopyOfContextMap());
    }

    @Override
    public Consumer<Optional<Map<String, String>>> copy() {

        return (map) -> map.ifPresent(MDC::setContextMap);
    }

    @Override
    public Consumer<Optional<Map<String, String>>> clear() {

        return (map) -> MDC.clear();
    }
}
