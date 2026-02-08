package com.atlas.common.redis.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Description
 * @Author ys
 * @Date 2025/12/10 15:13
 */
public class DateSequencePart implements SequencePart{

    private String datePattern;

    public DateSequencePart(String datePattern) {
        this.datePattern = datePattern;
    }

    @Override
    public String generate(String bizContent) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(datePattern));
    }
}
