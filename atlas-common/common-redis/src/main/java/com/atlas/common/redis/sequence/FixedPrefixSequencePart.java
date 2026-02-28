package com.atlas.common.redis.sequence;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/28 17:28
 */
public class FixedPrefixSequencePart implements SequencePart{

    private final String prefix;

    public FixedPrefixSequencePart(String prefix){
        this.prefix = prefix;
    }

    @Override
    public String generate(String bizContent) {

        return prefix == null ? "" : prefix;
    }

}
