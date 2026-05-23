package com.atlas.auth.config.security.webauthn;

import com.atlas.security.properties.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;

import java.time.Duration;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/13 11:57
 */
public class RedisPublicKeyCredentialCreationOptionsRepository implements PublicKeyCredentialCreationOptionsRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisPublicKeyCredentialCreationOptionsRepository.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;

    private static final String PREFIX = "webauthn:creation:user:";

    public RedisPublicKeyCredentialCreationOptionsRepository(RedisTemplate<String, Object> redisTemplate, SecurityProperties securityProperties){
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    @Override
    public void save(HttpServletRequest request, HttpServletResponse response, PublicKeyCredentialCreationOptions options) {
        if(options == null){
            log.warn("options is null");
            return;
        }
        String key = PREFIX + getIdentifier(request);

        WebAuthnCreationContext context = WebAuthnCreationContext.builder()
                .challenge(options.getChallenge().toBase64UrlString())
                .userId(options.getUser().getId().toBase64UrlString())
                .userName(options.getUser().getName())
                .userDisplayName(options.getUser().getDisplayName())
                .timeoutValue(options.getTimeout() != null ? options.getTimeout().toMillis() : 300000L)
                .attestation(options.getAttestation().getValue())
                .residentKey(
                        options.getAuthenticatorSelection()
                                .getResidentKey()
                                .getValue()
                )
                .userVerification(
                        options.getAuthenticatorSelection()
                                .getUserVerification()
                                .getValue()
                )
                .build();

        redisTemplate.opsForValue().set(key,context, Duration.ofMinutes(5));
    }

    @Override
    public PublicKeyCredentialCreationOptions load(HttpServletRequest request) {
        String key = PREFIX + getIdentifier(request);
        Object cachedValue = redisTemplate.opsForValue().get(key);
        WebAuthnCreationContext webAuthnCreationContext = (WebAuthnCreationContext) cachedValue;

        Bytes challengeBytes = Bytes.fromBase64(webAuthnCreationContext.getChallenge());

        PublicKeyCredentialRpEntity rp = PublicKeyCredentialRpEntity.builder()
                        .id(securityProperties.getWebauthn().getRpId())
                        .name(securityProperties.getWebauthn().getRpName())
                        .build();

        PublicKeyCredentialUserEntity user = ImmutablePublicKeyCredentialUserEntity.builder()
                        .id(Bytes.fromBase64(webAuthnCreationContext.getUserId()))
                        .name(webAuthnCreationContext.getUserName())
                        .displayName(webAuthnCreationContext.getUserDisplayName())
                        .build();


        return PublicKeyCredentialCreationOptions.builder()
                .rp(rp)
                .user(user)
                .challenge(challengeBytes)
                .pubKeyCredParams(List.of(
                        PublicKeyCredentialParameters.ES256,
                        PublicKeyCredentialParameters.RS256
                ))
                .timeout(Duration.ofMillis(webAuthnCreationContext.getTimeoutValue()))
                .attestation(AttestationConveyancePreference
                        .valueOf(
                                webAuthnCreationContext.getAttestation()
                        ))
                .authenticatorSelection(
                        AuthenticatorSelectionCriteria
                                .builder()
                                .residentKey(
                                        ResidentKeyRequirement
                                                .valueOf(
                                                        webAuthnCreationContext.getResidentKey()
                                                )
                                )
                                .userVerification(
                                        userVerification(webAuthnCreationContext.getUserVerification())
                                )
                                .build()
                )
                .build();

    }

    private String getIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            // 直接用用户名作为分布式存储的 Key
            return authentication.getName();
        }
        throw new AccessDeniedException("必须登录后才能开启 WebAuthn");
    }

    private UserVerificationRequirement userVerification(String value) {

        if (value == null) {
            return UserVerificationRequirement.PREFERRED;
        }

        return switch (value.toLowerCase()) {

            case "required" ->
                    UserVerificationRequirement.REQUIRED;

            case "discouraged" ->
                    UserVerificationRequirement.DISCOURAGED;

            default ->
                    UserVerificationRequirement.PREFERRED;
        };
    }
}
