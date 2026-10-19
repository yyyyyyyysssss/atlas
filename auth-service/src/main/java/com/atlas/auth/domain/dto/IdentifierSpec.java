package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.IdentifierType;

public record IdentifierSpec(
        IdentifierType type,
        String value,
        Boolean verified // 可为空
) {

    public static IdentifierSpec generatedUsername(){
        return new IdentifierSpec(IdentifierType.USERNAME, null, true);
    }

    public static IdentifierSpec email(String email, Boolean emailVerified){
        return new IdentifierSpec(IdentifierType.EMAIL, email, emailVerified);
    }

    public static IdentifierSpec phone(String phone, Boolean phoneVerified){
        return new IdentifierSpec(IdentifierType.PHONE, phone, phoneVerified);
    }

}
