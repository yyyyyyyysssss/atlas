package com.atlas.auth.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@RequiredArgsConstructor
public enum ClientProtocol implements IEnum<String> {


    OAUTH2("OAUTH2", "OAuth 2.0"),

    SAML2("SAML2", "SAML 2.0"),

    ;

    @EnumValue
    private final String value;

    private final String label;

    @Override
    public String getValue() {
        return value;
    }

}
