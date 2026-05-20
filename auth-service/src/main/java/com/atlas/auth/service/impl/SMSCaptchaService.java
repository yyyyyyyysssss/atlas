package com.atlas.auth.service.impl;

import com.atlas.auth.enums.CaptchaScene;
import com.atlas.auth.service.AbstractCaptchaService;
import com.atlas.common.core.api.notification.NotificationApi;
import com.atlas.common.core.api.notification.builder.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/18 15:39
 */
@Service("smsCaptchaService")
@RequiredArgsConstructor
public class SMSCaptchaService extends AbstractCaptchaService {

    private final NotificationApi notificationApi;

    @Override
    protected String getRedisKeyPrefix() {

        return "sms:code";
    }

    @Override
    protected void doSend(String target, String code, Duration duration, CaptchaScene scene) {
        //准备通知变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("code", code);
        variables.put("min", duration.toMinutes());
        // 发送
        notificationApi.send(
                NotificationRequest
                        .template(scene.getCode(), scene.getDescription(), variables)
                        .sms(sms -> {

                        })
                        .to()
                        .toPhones(target)
                        .build()
        );
    }

}
