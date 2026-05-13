package com.atlas.auth.config.security.webauthn;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/13 11:57
 */
public class RedisPublicKeyCredentialCreationOptionsRepository implements PublicKeyCredentialCreationOptionsRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "webauthn:creation:user:";

    public RedisPublicKeyCredentialCreationOptionsRepository(RedisTemplate<String, Object> redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(HttpServletRequest request, HttpServletResponse response, PublicKeyCredentialCreationOptions options) {
        String key = PREFIX + getIdentifier(request);
        redisTemplate.opsForValue().set(key,options, Duration.ofMinutes(5));
    }

    @Override
    public PublicKeyCredentialCreationOptions load(HttpServletRequest request) {
        String key = PREFIX + getIdentifier(request);
        return (PublicKeyCredentialCreationOptions)redisTemplate.opsForValue().get(key);
    }

    private String getIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            // 直接用用户名作为分布式存储的 Key
            return authentication.getName();
        }
        throw new AccessDeniedException("必须登录后才能开启 WebAuthn");
    }
}
