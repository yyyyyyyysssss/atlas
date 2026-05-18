package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.IdentifierType;

public record IdentifierSpec(
        IdentifierType type,
        String value,
        Boolean verified // 可为空
) {
}
