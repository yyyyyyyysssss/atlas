package com.atlas.security.autoconfigure;

import com.atlas.common.core.autoconfigure.AtlasCoreAutoConfiguration;
import com.atlas.common.redis.autoconfigure.AtlasRedisAutoConfiguration;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.handler.SecurityExceptionHandler;
import com.atlas.security.jackson.AuthorityUrlMixin;
import com.atlas.security.jackson.RequestUrlAuthorityMixin;
import com.atlas.security.model.AuthorityUrl;
import com.atlas.security.model.RequestUrlAuthority;
import com.atlas.security.properties.SecurityProperties;
import com.atlas.security.repository.RedisSecurityContextRepository;
import com.atlas.security.service.DefaultTokenService;
import com.atlas.security.service.TokenService;
import com.atlas.security.utils.JwtUtils;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.web.jackson2.WebServletJackson2Module;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 12:59
 */
@AutoConfiguration(after = {
        AtlasCoreAutoConfiguration.class,
        AtlasRedisAutoConfiguration.class
})
@Import({SecurityExceptionHandler.class})
@EnableConfigurationProperties(SecurityProperties.class)
public class AtlasSecurityAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public TokenService tokenService(JwtUtils jwtUtils, RedisHelper redisHelper){

        return new DefaultTokenService(jwtUtils,redisHelper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisSecurityContextRepository securityContextRepository(
            @Qualifier("securityRedisTemplate") RedisTemplate<String, SecurityContext> securityRedisTemplate,
            SecurityProperties securityProperties) {

        return new RedisSecurityContextRepository(securityRedisTemplate, securityProperties);
    }

    @Bean
    public RedisTemplate<String, SecurityContext> securityRedisTemplate(RedisConnectionFactory redisConnectionFactory, @Qualifier("securityObjectMapper") ObjectMapper securityObjectMapper) {
        RedisTemplate<String, SecurityContext> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(securityObjectMapper);

        redisTemplate.setValueSerializer(genericJackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(genericJackson2JsonRedisSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean(name = "securityObjectMapper") // 明确命名，避免覆盖默认 Bean
    public ObjectMapper securityObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 注册基础模块
        objectMapper.registerModule(new JavaTimeModule());
        // 注意：CoreJackson2Module 和 WebServletJackson2Module 是 Spring Security 提供的
        // 它们允许 Jackson 识别 SimpleGrantedAuthority 等安全类
        objectMapper.registerModule(new CoreJackson2Module());
        objectMapper.registerModule(new WebServletJackson2Module());

        // 2. 注册你自定义的权限模型 Mixin
        objectMapper.addMixIn(RequestUrlAuthority.class, RequestUrlAuthorityMixin.class);
        objectMapper.addMixIn(AuthorityUrl.class, AuthorityUrlMixin.class);

        // 3. 必须开启的配置：保留类型信息
        // 否则 Redis 反序列化时不知道要把 JSON 转成哪个具体的实现类
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }

    @Bean
    public JwtUtils jwtUtils(SecurityProperties securityProperties){

        return new JwtUtils(securityProperties);
    }

}
