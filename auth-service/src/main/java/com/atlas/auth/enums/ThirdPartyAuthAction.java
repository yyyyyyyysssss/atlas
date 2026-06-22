package com.atlas.auth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ThirdPartyAuthAction {

    LOGIN,

    BIND;

    @JsonCreator
    public static ThirdPartyAuthAction fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return ThirdPartyAuthAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOGIN;
        }
    }

}
