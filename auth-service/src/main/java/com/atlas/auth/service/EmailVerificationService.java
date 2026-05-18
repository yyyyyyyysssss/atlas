package com.atlas.auth.service;

import com.atlas.auth.enums.VerificationScene;
import com.atlas.common.core.api.notification.NotificationApi;
import com.atlas.common.core.api.notification.builder.NotificationRequest;
import com.atlas.common.core.utils.VerificationCodeUtils;
import com.atlas.common.redis.utils.RedisHelper;
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
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisHelper redisHelper;

    private final NotificationApi notificationApi;

    private static final String CODE_PREFIX = "email:verification:code:";

    public void send(String email, VerificationScene verificationScene) {
        send(email, verificationScene, Duration.ofMinutes(10));
    }

    public void send(String email, VerificationScene verificationScene, Duration duration) {
        String redisKey = CODE_PREFIX + verificationScene.getLowerCaseCode() + ":" + email;
        // 生成并存入 Redis
        String code = VerificationCodeUtils.genVerificationCode();
        redisHelper.setValue(redisKey, code, duration);

        //准备通知变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("code", code);
        variables.put("min", duration.toMinutes());
        // 发送
        notificationApi.send(
                NotificationRequest
                        .template(verificationScene.getCode(), verificationScene.getDescription(), variables)
                        .email()
                        .to()
                        .toEmails(email)
                        .build()
        );
    }

    public boolean verify(String email, String inputCode, VerificationScene verificationScene) {
        String redisKey = CODE_PREFIX + verificationScene.getLowerCaseCode() + ":" + email;
        String cacheCode = redisHelper.getValue(redisKey, String.class);
        if (cacheCode == null || !cacheCode.equals(inputCode)) {
            return false;
        }
        redisHelper.delete(CODE_PREFIX + email);
        return true;
    }

}
