package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ThirdPartyLoginDTO;
import com.atlas.auth.domain.dto.ThirdPartyUserIdentity;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.utils.TicketGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/15 16:07
 */
public abstract class AbstractThirdPartyLoginProvider implements ThirdPartyLoginProvider {

    @Resource
    private LoginService loginService;

    @Resource
    private UserService userService;

    @Resource
    protected SsoProviderService ssoProviderService;

    @Resource
    protected OAuth2ProviderEngine oAuth2ProviderEngine;

    @Resource
    private RedisHelper redisHelper;

    private final static String OAUTH2_STATE_PREFIX_KEY = "oauth2:state:";

    protected final String generateState(ThirdPartyAuthAction action) {
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

        ThirdPartyStateContext stateContext = new ThirdPartyStateContext(getProviderName(), action, currentUserId);
        redisHelper.setValue(key, stateContext, Duration.ofMinutes(5));
        return state;
    }

    protected final ThirdPartyStateContext validateState(String state) {
        String key = OAUTH2_STATE_PREFIX_KEY + state;
        try {
            ThirdPartyStateContext stateContext = redisHelper.getValue(key, new TypeReference<ThirdPartyStateContext>() {});
            String providerName = stateContext.provider;
            if(!StringUtils.hasText(providerName) || !providerName.equals(getProviderName())){
                throw new BusinessException("非法登录请求，State 校验失败");
            }
            return stateContext;
        }finally {
            redisHelper.delete(key);
        }
    }

    protected TokenResponse dispatchFederatedIdentity(ThirdPartyUserIdentity thirdPartyUserIdentity, ThirdPartyStateContext stateContext){

        return switch (stateContext.action) {
            case BIND -> doBind(thirdPartyUserIdentity, stateContext.userId);
            default -> doLogin(thirdPartyUserIdentity);
        };
    }

    protected TokenResponse doLogin(ThirdPartyUserIdentity thirdPartyUserIdentity){
        String provider = thirdPartyUserIdentity.getProvider();
        Long userId = userService.ensureUserByProvider(provider, thirdPartyUserIdentity);
        ThirdPartyLoginDTO thirdPartyLoginDTO = new ThirdPartyLoginDTO(ClientType.WEB, userId);
        return loginService.loginThirdParty(thirdPartyLoginDTO);
    }

    protected TokenResponse doBind(ThirdPartyUserIdentity thirdPartyUserIdentity, Long currentUserId){
        if (currentUserId == null) {
            throw new BusinessException("账号关联失败：未获取到当前账户登录凭证");
        }
        String provider = thirdPartyUserIdentity.getProvider();
        userService.bindThirdPartyProvider(provider,currentUserId, thirdPartyUserIdentity);
        return TokenResponse.successBind();
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ThirdPartyStateContext{
        private String provider;
        private ThirdPartyAuthAction action;
        private Long userId;
    }

}
