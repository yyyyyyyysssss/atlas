package com.atlas.common.core.thread;

import com.atlas.common.core.constant.CommonConstant;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.UUID;

/**
 * @Description 解决子父线程链路跟踪id传递问题
 * @Author ys
 * @Date 2025/2/08 15:18
 */
@Slf4j
public class MDCTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(@Nonnull Runnable runnable) {
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (copyOfContextMap != null) {
                    MDC.setContextMap(copyOfContextMap);
                } else {
                    String traceId = UUID.randomUUID().toString().replaceAll("-", "");
                    MDC.put(CommonConstant.TRACE_ID, traceId);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }

        };
    }
}
