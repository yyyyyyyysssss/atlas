package com.atlas.common.redis.sequence;

public interface SequencePart {

    String generate(String bizContent);

}
