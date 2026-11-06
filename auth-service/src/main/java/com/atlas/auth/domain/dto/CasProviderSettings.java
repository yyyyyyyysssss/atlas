package com.atlas.auth.domain.dto;

public record CasProviderSettings(

        // CAS 登录地址
        String loginUrl,

        String serviceValidateUrl,

        // 当前 CAS Client 注册的 Service 地址
        String serviceUrl,

        String proxyUrl,

        String proxyValidateUrl,

        Mapping mappings
) implements SsoSettings {


    public record Mapping(
            String sub,
            String email,
            String emailVerified,
            String fullName,
            String avatar
    ) {
    }

}
