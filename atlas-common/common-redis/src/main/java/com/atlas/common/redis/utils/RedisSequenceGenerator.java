package com.atlas.common.redis.utils;

import com.atlas.common.core.idwork.SequenceGenerator;

import java.util.List;


/**
 * @Description
 * @Author ys
 * @Date 2025/12/10 14:10
 */
public class RedisSequenceGenerator implements SequenceGenerator {

    private List<SequencePart> sequenceParts;

    public RedisSequenceGenerator(List<SequencePart> sequenceParts) {
        this.sequenceParts = sequenceParts;
    }

    @Override
    public String generate(String bizPrefix) {
        StringBuilder builder = new StringBuilder();
        // bizPrefix作为前缀
        builder.append(bizPrefix);
        // 添加其它部分
        for (SequencePart part : sequenceParts) {
            builder.append(part.generate(bizPrefix));
        }
        return builder.toString();
    }


}
