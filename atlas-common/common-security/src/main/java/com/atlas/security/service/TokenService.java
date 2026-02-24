package com.atlas.security.service;

import com.atlas.security.enums.ClientType;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.enums.TokenType;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.model.TokenResponse;

public interface TokenService {

    TokenResponse createToken(SecurityUser securityUser, ClientType clientType, boolean refreshFlag, boolean rememberMeFlag);

    PayloadInfo verify(String token, TokenType tokenType);

    void revoke(String token);

}
