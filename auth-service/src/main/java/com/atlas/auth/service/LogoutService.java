package com.atlas.auth.service;

import com.atlas.common.core.context.UserContext;
import com.atlas.security.repository.SecurityContextStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogoutService {

    @Resource
    private SecurityContextStore securityContextRepository;
    
    public void logout(String tokenId) {
        Long userId = UserContext.getUserId();
        // 移除 TokenID 对应的 SecurityContext
        securityContextRepository.clearContext(tokenId);
    }

}
