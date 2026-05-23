package com.atlas.auth.service;

import com.atlas.auth.enums.SecurityScene;
import com.atlas.common.core.utils.VerificationCodeUtils;
import com.atlas.common.redis.utils.RedisHelper;
import jakarta.annotation.Resource;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/20 10:20
 */

public abstract class AbstractCaptchaService implements CaptchaVerificationService{

    @Resource
    protected RedisHelper redisHelper;

    private final String prefix = "captcha";

    // 由子类定义自己独特的 Redis 前缀，例如 "email:code:" 或 "sms:code:"
    protected abstract String getRedisKeyPrefix();

    protected abstract void doSend(String target, String code, Duration duration, SecurityScene scene);

    @Override
    public void send(String target, SecurityScene scene) {
        send(target, scene, Duration.ofMinutes(10));
    }

    @Override
    public void send(String target, SecurityScene scene, Duration duration) {
        String redisKey = buildRedisKey(target, scene);
        String code = VerificationCodeUtils.genVerificationCode();
        redisHelper.setValue(redisKey, code, duration);
        // 触发子类的具体发送
        doSend(target, code, duration, scene);
    }

    @Override
    public boolean verify(String target, String inputCode, SecurityScene scene) {
        String redisKey = buildRedisKey(target, scene);
        String cacheCode = redisHelper.getValue(redisKey, String.class);
        if (cacheCode == null || !cacheCode.equals(inputCode)) {
            return false;
        }
        // 修复了你原代码里的那个 Key 拼接 Bug
        redisHelper.delete(redisKey);
        return true;
    }

    private String buildRedisKey(String target, SecurityScene scene) {
        String redisKeyPrefix = getRedisKeyPrefix();
        if (redisKeyPrefix == null || redisKeyPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("验证码[%s] keyPrefix 不能为空！", this.getClass().getSimpleName())
            );
        }
        if(!redisKeyPrefix.startsWith(":")){
            redisKeyPrefix = ":" + redisKeyPrefix;
        }
        if(!redisKeyPrefix.endsWith(":")){
            redisKeyPrefix = redisKeyPrefix + ":";
        }
        return prefix + redisKeyPrefix + scene.getLowerCaseCode() + ":" + target;
    }

}
