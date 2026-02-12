package com.atlas.common.core.api.notification.feign;


import com.atlas.common.core.api.notification.dto.NotificationDTO;
import com.atlas.common.core.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notificationFeignApi",
        path = "/v1/notifications",
        url = "${atlas.notification.server-url:}",
        fallbackFactory = NotificationFallbackFactory.class
)
public interface NotificationFeignApi {

    @PostMapping("/send")
    void send(@RequestBody NotificationDTO notification);

    @GetMapping("/test")
    Result<?> test();

}
