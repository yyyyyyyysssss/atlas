package com.atlas.user.config.sequence;

import com.atlas.common.core.idwork.SequenceGenerator;
import com.atlas.common.redis.sequence.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/28 17:33
 */
@Configuration
public class SequenceConfig {

    @Bean("orgSequenceGenerator")
    public SequenceGenerator orgSequenceGenerator(RedisTemplate<String, Object> redisTemplate){
        SequencePart fixedPrefixSequencePart = new FixedPrefixSequencePart("1");
        SequencePart sequencePart = new SequenceNumberPart(redisTemplate, 5);
        return new RedisSequenceGenerator(Arrays.asList(fixedPrefixSequencePart,sequencePart));
    }

}
