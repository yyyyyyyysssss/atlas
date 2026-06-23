package com.atlas.auth.enums;

import com.atlas.auth.domain.dto.OAuth2ProviderSettings;
import com.atlas.auth.domain.dto.OidcProviderSettings;
import com.atlas.auth.domain.dto.Saml2ProviderSettings;
import com.atlas.auth.domain.dto.SsoSettings;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public enum SsoProviderProtocol {

    OAUTH2(OAuth2ProviderSettings.class),
    SAML2(Saml2ProviderSettings.class),
    OIDC(OidcProviderSettings.class)

    ;

    private final Class<? extends SsoSettings> settingsClass;

    // 构造函数
    SsoProviderProtocol(Class<? extends SsoSettings> settingsClass) {
        this.settingsClass = settingsClass;
    }

    @JsonCreator
    public static SsoProviderProtocol fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // 核心：去除空格并转为大写后再匹配
            return SsoProviderProtocol.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("收到未知的协议类型: {}", value);
            return SsoProviderProtocol.OAUTH2;
        }
    }
}
