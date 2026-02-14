package com.atlas.security.service;

import com.atlas.security.model.PayloadInfo;
import com.atlas.security.enums.TokenType;

public interface TokenService {

    PayloadInfo verify(String token);

}
