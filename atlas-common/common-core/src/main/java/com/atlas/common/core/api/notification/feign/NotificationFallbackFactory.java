package com.atlas.common.core.api.notification.feign;

import com.atlas.common.core.api.feign.factory.BaseFallbackFactory;
import com.atlas.common.core.api.notification.dto.NotificationDTO;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/12 11:29
 */
@Component
@Slf4j
public class NotificationFallbackFactory implements BaseFallbackFactory<NotificationFeignApi> {


    @Override
    public NotificationFeignApi createFallback(Throwable cause) {
        // 这里统一记录调用异常的原因
        log.error("Notification Feign Fallback: {}", cause.getMessage());

        return new NotificationFeignApi() {
            @Override
            public void send(NotificationDTO notification) {
                log.warn("降级逻辑：忽略该通知发送，Subject: {}", notification.getTitle());
            }

            @Override
            public Result<?> test() {
                log.warn("test fallback");
                return ResultGenerator.failed("test fallback");
            }
        };
    }
}
