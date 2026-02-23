package com.atlas.security.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenScheme {

    BEARER("Bearer"),
    ;

    @JsonValue
    private final String value;

    /**
     * 获取带空格的头前缀，例如 "Bearer "
     */
    public String getPrefix() {
        return this.value + " ";
    }

}
