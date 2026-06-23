package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ThirdPartyStateContext;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.utils.TicketGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/23 15:57
 */
@Service
@Slf4j
public class ThirdPartyStateService {

    @Resource
    private RedisHelper redisHelper;

    private final static String OAUTH2_STATE_PREFIX_KEY = "oauth2:state:";

    public String generateState(String providerName, ThirdPartyAuthAction action, SsoProviderProtocol protocol){
        Long currentUserId = null;
        if(ThirdPartyAuthAction.BIND.equals(action)){
            SecurityContext securityContext = SecurityContextHolder.getContext();
            if(securityContext == null || !securityContext.getAuthentication().isAuthenticated()){
                throw new BusinessException("账户未登录，无法发起第三方账号绑定");
            }
            SecurityUser securityUser = (SecurityUser) securityContext.getAuthentication().getPrincipal();
            currentUserId = securityUser.getId();
        }
        String state = TicketGenerator.generate(32);
        String key = OAUTH2_STATE_PREFIX_KEY + state;

        ThirdPartyStateContext stateContext = new ThirdPartyStateContext(providerName, action, currentUserId, protocol);
        redisHelper.setValue(key, stateContext, Duration.ofMinutes(5));
        return state;
    }

    public ThirdPartyStateContext peekContext(String state) {
        String key = OAUTH2_STATE_PREFIX_KEY + state;
        ThirdPartyStateContext stateContext = redisHelper.getValue(key, new TypeReference<ThirdPartyStateContext>() {});
        if (stateContext == null) {
            throw new BusinessException("登录超时或非法单点登录请求");
        }
        return stateContext;
    }

    public final ThirdPartyStateContext validateState(String state, String expectedProvider) {
        String key = OAUTH2_STATE_PREFIX_KEY + state;
        try {
            ThirdPartyStateContext stateContext = redisHelper.getValue(key, new TypeReference<ThirdPartyStateContext>() {});
            String providerName = stateContext.getProvider();
            if(!StringUtils.hasText(providerName) || !providerName.equals(expectedProvider)){
                throw new BusinessException("非法登录请求，State 校验失败");
            }
            return stateContext;
        }finally {
            redisHelper.delete(key);
        }
    }

}
