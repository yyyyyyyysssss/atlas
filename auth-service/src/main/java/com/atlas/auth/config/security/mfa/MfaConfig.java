package com.atlas.auth.config.security.mfa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class MfaConfig {


    @Bean
    public MfaTicketRepository mfaTicketRepository(RedisTemplate<String, Object> securityRedisTemplate){

        return new RedisMfaTicketRepository(securityRedisTemplate);
    }

}
