package com.atlas.auth.config.security.webauthn;

import com.atlas.security.properties.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/13 11:57
 */
@Slf4j
public class RedisPublicKeyCredentialCreationOptionsRepository implements PublicKeyCredentialCreationOptionsRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;

    private static final String PREFIX = "webauthn:credential:creation:";

    public RedisPublicKeyCredentialCreationOptionsRepository(RedisTemplate<String, Object> redisTemplate, SecurityProperties securityProperties) {
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    @Override
    public void save(HttpServletRequest request, HttpServletResponse response, PublicKeyCredentialCreationOptions options) {
        if (options == null) {
            log.warn("options is null");
            return;
        }
        String key = PREFIX + resolveWebauthnId(request);

        WebauthnCredentialOptionsContext context = WebauthnCredentialOptionsContext.of(options);

        redisTemplate.opsForValue().set(key, context, Duration.ofMinutes(5));
    }

    @Override
    public PublicKeyCredentialCreationOptions load(HttpServletRequest request) {
        String key = PREFIX + resolveWebauthnId(request);
        Object cachedValue = redisTemplate.opsForValue().get(key);
        if (!(cachedValue instanceof WebauthnCredentialOptionsContext webAuthnCreationContext)) {
            return null;
        }
        return webAuthnCreationContext.toPublicKeyCredentialCreationOptions(securityProperties);
    }

    public void remove(HttpServletRequest request){
        String key = PREFIX + resolveWebauthnId(request);
        redisTemplate.delete(key);
    }

    private String resolveWebauthnId(HttpServletRequest request) {
        String webauthnId = request.getHeader("X-Webauthn-Id");
        if (webauthnId == null || webauthnId.isBlank()) {
            webauthnId = (String) request.getAttribute("webauthnId");
        }
        if(webauthnId == null || webauthnId.isBlank()){
            throw new AccessDeniedException("无法建立有效的 Webauthn 挑战上下文追踪标识");
        }
        return webauthnId;
    }
}
