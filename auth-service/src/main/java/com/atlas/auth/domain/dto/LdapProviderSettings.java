package com.atlas.auth.domain.dto;

import com.atlas.common.crypto.symmetric.AESUtils;

import java.util.Objects;

public record LdapProviderSettings(

        String url,

        String baseDn,

        String bindDn,

        String bindPassword,

        UserSearch userSearch

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
                userSearch
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
                userSearch
        );
    }


    public record UserSearch(
            String base,
            String filter
    ) {
    }
}
