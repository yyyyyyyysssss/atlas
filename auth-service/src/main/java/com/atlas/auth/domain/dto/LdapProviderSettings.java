package com.atlas.auth.domain.dto;

import com.atlas.common.crypto.symmetric.AESUtils;

import java.util.Objects;

public record LdapProviderSettings(

        String url,

        String baseDn,

        String bindDn,

        String bindPassword,

        UserSearch userSearch,

        Mapping mappings

) implements SsoSettings, Decryptable<LdapProviderSettings>, Encryptable<LdapProviderSettings> {


    @Override
    public LdapProviderSettings decrypt(String key) {
        Objects.requireNonNull(key, "key must not be null");
        String decrypt = bindPassword;
        if(bindPassword != null && !bindPassword.isBlank()){
            decrypt = AESUtils.decrypt(bindPassword, key);
        }
        return new LdapProviderSettings(
                url,
                baseDn,
                bindDn,
                decrypt,
                userSearch,
                mappings
        );
    }

    @Override
    public LdapProviderSettings encrypt(String key) {
        Objects.requireNonNull(key, "key must not be null");
        String encrypt = AESUtils.encrypt(bindPassword, key);
        return new LdapProviderSettings(
                url,
                baseDn,
                bindDn,
                encrypt,
                userSearch,
                mappings
        );
    }


    public record UserSearch(
            String base,
            String filter
    ) {
    }

    public record Mapping(
            String sub,
            String email,
            String phone,
            String fullName
    ) {
    }
}
