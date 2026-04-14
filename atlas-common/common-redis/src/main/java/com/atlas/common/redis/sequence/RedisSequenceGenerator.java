package com.atlas.common.redis.sequence;

import com.atlas.common.core.idwork.SequenceGenerator;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * @Description
 * @Author ys
 * @Date 2025/12/10 14:10
 */
public class RedisSequenceGenerator implements SequenceGenerator {

    private List<SequencePart> sequenceParts;

    private String bizPrefix;

    public RedisSequenceGenerator(List<SequencePart> sequenceParts) {
        this(null,sequenceParts);
    }

    public RedisSequenceGenerator(String bizPrefix, List<SequencePart> sequenceParts) {
        this.bizPrefix = bizPrefix;
        this.sequenceParts = sequenceParts;
    }

    @Override
    public String generate(String bizPrefix) {
        String prefix = bizPrefix;
        if(StringUtils.isEmpty(prefix)){
            prefix = this.bizPrefix;
        }
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
