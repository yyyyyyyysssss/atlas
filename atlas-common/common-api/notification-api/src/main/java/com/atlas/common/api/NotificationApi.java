package com.atlas.common.api;


import com.atlas.common.api.builder.NotificationRequest;
import com.atlas.common.api.dto.NotificationDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

// 推荐使用 NotificationRequest 构建请求参数
@HttpExchange(url = "/v1/notifications")
public interface NotificationApi {

    @PostExchange("/send")
    void send(@RequestBody NotificationDTO notification);

    default void send(NotificationRequest.NotificationOp op) {
        if (op != null) {
            this.send(op.build());
        }
    }

}
