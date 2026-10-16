package com.atlas.auth.domain.dto;

public record LdapLoginRequest(
        String username,
        String password
) implements ThirdPartyLoginRequest {

}
