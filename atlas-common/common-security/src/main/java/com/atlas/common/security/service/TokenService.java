package com.atlas.common.security.service;

import com.atlas.common.security.enums.ClientType;
import com.atlas.common.security.enums.TokenType;
import com.atlas.common.security.model.PayloadInfo;
import com.atlas.common.security.model.SecurityUser;
import com.atlas.common.security.model.TokenInfo;

import java.util.function.Function;

public interface TokenService {

    TokenInfo createToken(SecurityUser securityUser, ClientType clientType, boolean refreshFlag);

    PayloadInfo verify(String token, TokenType tokenType);

    void revoke(String token);

    void revoke(String tokenId, Long expiration);

    <T> T extractInfo(String token, Function<PayloadInfo, T> extractor);

}
