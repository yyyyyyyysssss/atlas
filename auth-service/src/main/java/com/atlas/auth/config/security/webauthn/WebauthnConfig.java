package com.atlas.auth.config.security.webauthn;

import com.atlas.auth.service.UserWebauthnCredentialsService;
import com.atlas.security.properties.SecurityProperties;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/26 15:17
 */
@Configuration
public class WebauthnConfig {

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private UserWebauthnCredentialsService userWebauthnCredentialsService;

    @Bean
    public UserCredentialRepository userCredentialRepository() {
        return userWebauthnCredentialsService;
    }

    @Bean
    public PublicKeyCredentialCreationOptionsRepository publicKeyCredentialCreationOptionsRepository(RedisTemplate<String, Object> securityRedisTemplate) {

        return new RedisPublicKeyCredentialCreationOptionsRepository(securityRedisTemplate, securityProperties);
    }

    @Bean
    public PublicKeyCredentialRequestOptionsRepository publicKeyCredentialRequestOptionsRepository(RedisTemplate<String, Object> securityRedisTemplate) {
        return new RedisPublicKeyCredentialRequestOptionsRepository(securityRedisTemplate, securityProperties);
    }

    @Bean
    public Webauthn4JRelyingPartyOperations webauthn4JRelyingPartyOperations(PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository) {
        PublicKeyCredentialRpEntity rp = PublicKeyCredentialRpEntity.builder()
                .id(securityProperties.getWebauthn().getRpId())
                .name(securityProperties.getWebauthn().getRpName())
                .build();
        return new Webauthn4JRelyingPartyOperations(
                publicKeyCredentialUserEntityRepository,
                userWebauthnCredentialsService, rp, securityProperties.getWebauthn().getOrigins()
        );
    }

}
