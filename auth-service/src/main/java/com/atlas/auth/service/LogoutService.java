package com.atlas.auth.service;

import com.atlas.security.service.TokenService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogoutService {

    @Resource
    private TokenService tokenService;
    
    public void logout(String tokenId) {
        tokenService.revoke(tokenId);
    }

}
