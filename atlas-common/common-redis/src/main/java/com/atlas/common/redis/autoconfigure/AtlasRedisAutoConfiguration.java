package com.atlas.common.redis.autoconfigure;

import com.atlas.common.core.idwork.SequenceGenerator;
import com.atlas.common.core.json.JacksonConfig;
import com.atlas.common.redis.utils.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.StatefulConnection;
import jakarta.annotation.Resource;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Description
 * @Author ys
 * @Date 2024/7/8 16:59
 */
@AutoConfiguration(after = {JacksonConfig.class})
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RedisProperties.class)
public class AtlasRedisAutoConfiguration {

    @Value("${spring.application.name:atlas}")
    private String applicationName;

    @Resource
    private RedisProperties redisProperties;

    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redisConnectionFactory(){
        RedisConfiguration redisConfig;
        //  哨兵模式
        if (redisProperties.getSentinel() != null && !redisProperties.getSentinel().getNodes().isEmpty()) {
            redisConfig = createSentinelConfig();
        } else if(redisProperties.getCluster() != null && !redisProperties.getCluster().getNodes().isEmpty()){
            // 集群模式
            redisConfig = createClusterConfig();
        } else {
            // 单机模式
            redisConfig = createStandaloneConfig();
        }

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration
                .builder()
                .poolConfig(createPoolConfig())
                .build();
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    // 单机配置
    private RedisStandaloneConfiguration createStandaloneConfig(){
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());
        if (redisProperties.getPassword() != null) {
            config.setPassword(redisProperties.getPassword());
        }
        config.setDatabase(redisProperties.getDatabase());
        return config;
    }

    // 哨兵配置
    private RedisSentinelConfiguration createSentinelConfig() {
        RedisProperties.Sentinel sentinel = redisProperties.getSentinel();
        Set<String> nodeSet = new HashSet<>(sentinel.getNodes());
        RedisSentinelConfiguration config = new RedisSentinelConfiguration(sentinel.getMaster(), nodeSet);
        if (redisProperties.getPassword() != null) {
            config.setPassword(redisProperties.getPassword());
        }
        config.setDatabase(redisProperties.getDatabase());
        return config;
    }

    // 集群配置
    private RedisClusterConfiguration createClusterConfig() {
        RedisProperties.Cluster cluster = redisProperties.getCluster();
        RedisClusterConfiguration config = new RedisClusterConfiguration(cluster.getNodes());
        if (redisProperties.getPassword() != null) {
            config.setPassword(redisProperties.getPassword());
        }
        return config;
    }

    private GenericObjectPoolConfig<StatefulConnection<?, ?>> createPoolConfig() {
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        RedisProperties.Pool pool = redisProperties.getLettuce().getPool();
        if (pool != null) {
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMinIdle(pool.getMinIdle());
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxWait(pool.getMaxWait());
        }
        return poolConfig;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory, GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        redisTemplate.setValueSerializer(genericJackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(genericJackson2JsonRedisSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer(ObjectMapper objectMapper) {

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory,GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer) {

        return RedisCacheManager
                .builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration(Duration.ofHours(1), genericJackson2JsonRedisSerializer))
                .build();

    }

    @Bean
    public RedisHelper redisHelper(RedisTemplate<String, Object> redisTemplate, GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer, ObjectMapper objectMapper) {
        return new RedisHelper(redisTemplate,genericJackson2JsonRedisSerializer,objectMapper);
    }

    @Bean("timeSequenceGenerator")
    @Primary
    public SequenceGenerator timeSequenceGenerator(RedisTemplate<String, Object> redisTemplate){
        SequencePart datePart = new DateSequencePart("yyyyMMdd");
        SequencePart sequencePart = new SequenceNumberPart(redisTemplate, 6);
        SequencePart suffixPart = new SuffixSequencePart();
        return new RedisSequenceGenerator(Arrays.asList(datePart,sequencePart,suffixPart));
    }

    @Bean("numberSequenceGenerator")
    public SequenceGenerator numberSequenceGenerator(RedisTemplate<String, Object> redisTemplate){
        SequencePart sequencePart = new SequenceNumberPart(redisTemplate, 3);
        return new RedisSequenceGenerator(List.of(sequencePart));
    }


    private RedisCacheConfiguration redisCacheConfiguration(Duration duration,GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer) {
        return RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(duration) // 默认1小时过期
                .computePrefixWith(cacheName -> applicationName  + ":" + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer))
                .disableCachingNullValues();// 关闭缓存null
    }



}
