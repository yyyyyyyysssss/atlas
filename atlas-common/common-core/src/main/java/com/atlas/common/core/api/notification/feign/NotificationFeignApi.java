package com.atlas.common.core.api.notification.feign;


import com.atlas.common.core.api.notification.builder.NotificationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notificationFeignApi",
        path = "/v1/notification",
        url = "${atlas.notification.server-url:}",
        fallbackFactory = NotificationFallbackFactory.class
)
public interface NotificationFeignApi {

    @PostMapping("/send")
    void send(@RequestBody NotificationDTO notification);

}
