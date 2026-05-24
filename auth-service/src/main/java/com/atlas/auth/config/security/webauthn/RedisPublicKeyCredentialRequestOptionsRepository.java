package com.atlas.auth.config.security.webauthn;

import com.atlas.security.model.SecurityUser;
import com.atlas.security.properties.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;

import java.time.Duration;

@Slf4j
public class RedisPublicKeyCredentialRequestOptionsRepository implements PublicKeyCredentialRequestOptionsRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;

    private static final String PREFIX = "webauthn:credential:request:";

    public RedisPublicKeyCredentialRequestOptionsRepository(RedisTemplate<String, Object> redisTemplate, SecurityProperties securityProperties){
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    @Override
    public void save(HttpServletRequest request, HttpServletResponse response, PublicKeyCredentialRequestOptions options) {
        if(options == null){
            log.warn("options is null");
            return;
        }
        String key = PREFIX + getIdentifier(request);

        WebauthnCredentialOptionsContext context = WebauthnCredentialOptionsContext.of(options);

        redisTemplate.opsForValue().set(key,context, Duration.ofMinutes(5));
    }

    @Override
    public PublicKeyCredentialRequestOptions load(HttpServletRequest request) {
        String key = PREFIX + getIdentifier(request);
        Object cachedValue = redisTemplate.opsForValue().get(key);
        if(!(cachedValue instanceof WebauthnCredentialOptionsContext webAuthnCreationContext)){
            return null;
        }
        return webAuthnCreationContext.toPublicKeyCredentialRequestOptions(securityProperties);
    }

    public void remove(HttpServletRequest request){
        String key = PREFIX + getIdentifier(request);
        redisTemplate.delete(key);
    }

    private String getIdentifier(HttpServletRequest request) {
        // 如果已经是登录状态（如：绑定新设备、高危操作二次验证），优先用已登录的 userId
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
                return "userId:" + securityUser.getId();
            }
            if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                return "username:" + userDetails.getUsername();
            }
        }
        // 未登录状态下，尝试从请求体/参数中获取用户输入的用户名/账号（显式免密登录场景）
        String username = request.getParameter("username");
        if (username == null || username.isBlank()) {
            // 兼容从 Attribute 中获取（如果你的前端过滤器或者 Gateway 提前解析了 Body 并塞进了属性里）
            username = (String) request.getAttribute("username");
        }
        if (username != null && !username.isBlank()) {
            return "username:" + username;
        }
        // 纯无名 Passkey 登录（不输用户名直接刷脸）
        String webauthnId = request.getHeader("X-Webauthn-Id");
        if (webauthnId != null && !webauthnId.isBlank()) {
            return "webauthnId:" + webauthnId;
        }
        throw new AccessDeniedException("无法建立有效的 Webauthn 挑战上下文追踪标识");
    }
}
