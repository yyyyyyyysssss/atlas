package com.atlas.auth.domain.dto;

import com.atlas.security.model.MfaType;

import java.util.Set;

public record UserMfaInfo(

        Set<MfaType> enabledTypes,

        MfaType preferredMfaType
) {

    public boolean enabled(){
        return !enabledTypes.isEmpty();
    }

}
