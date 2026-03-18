package com.atlas.common.core.api.notification;


import com.atlas.common.core.api.notification.dto.NotificationDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

// 推荐使用 NotificationRequest 构建请求参数
@HttpExchange(url = "/v1/notification")
public interface NotificationApi {

    @PostExchange("/send")
    void send(@RequestBody NotificationDTO notification);

}
