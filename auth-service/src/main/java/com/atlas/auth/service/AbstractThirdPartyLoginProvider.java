package com.atlas.auth.service;

import com.atlas.auth.domain.dto.OAuth2UserInfo;
import com.atlas.auth.domain.dto.ThirdPartyLoginDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.utils.TicketGenerator;
import jakarta.annotation.Resource;
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

    protected final String generateState() {
        String state = TicketGenerator.generate(32);
        redisHelper.setValue(OAUTH2_STATE_PREFIX_KEY + state, getProviderName(), Duration.ofMinutes(5));
        return state;
    }

    protected final void validateState(String state) {
        String key = OAUTH2_STATE_PREFIX_KEY + state;
        try {
            String providerName = redisHelper.getValue(key,String.class);
            if(!StringUtils.hasText(providerName) || !providerName.equals(getProviderName())){
                throw new BusinessException("非法登录请求，State 校验失败");
            }
        }finally {
            redisHelper.delete(key);
        }
    }

    protected TokenResponse doLogin(OAuth2UserInfo oAuth2UserInfo){
        String provider = oAuth2UserInfo.getProvider();
        Long userId = userService.ensureUserByProvider(provider, oAuth2UserInfo);
        ThirdPartyLoginDTO thirdPartyLoginDTO = new ThirdPartyLoginDTO(ClientType.WEB, userId);
        return loginService.loginThirdParty(thirdPartyLoginDTO);
    }

}
