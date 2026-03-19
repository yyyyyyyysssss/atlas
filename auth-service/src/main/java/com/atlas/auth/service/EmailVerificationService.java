package com.atlas.auth.service;

import com.atlas.common.core.api.notification.NotificationApi;
import com.atlas.common.core.api.notification.builder.NotificationRequest;
import com.atlas.common.core.api.notification.dto.NotificationDTO;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.utils.VerificationCodeUtils;
import com.atlas.common.redis.utils.RedisHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    private final UserApi userApi;

    private static final String CODE_PREFIX = "email:verification:code:";

    public void send(String email) {
        Result<List<UserDTO>> result = userApi.findByEmails(Collections.singletonList(email));
        if(!result.isSucceed() || CollectionUtils.isEmpty(result.getData())){
            throw new BusinessException("用户不存在");
        }
        // 生成并存入 Redis
        String code = VerificationCodeUtils.genVerificationCode();
        redisHelper.setValue(CODE_PREFIX + email, code, Duration.ofMinutes(10));

        // 发送通知
        Map<String, Object> variable = new HashMap<>();
        variable.put("code", code);
        NotificationDTO dto = NotificationRequest
                .template("auth-code-email", "邮箱验证码", variable)
                .email()
                .withParam("min", 10)
                .to()
                .toEmails(email)
                .build();
        notificationApi.send(dto);
    }

    public boolean verify(String email, String inputCode) {
        String cacheCode = redisHelper.getValue(CODE_PREFIX + email,String.class);
        if (cacheCode == null || !cacheCode.equals(inputCode)) {
            return false;
        }
        redisHelper.delete(CODE_PREFIX + email);
        return true;
    }

}
