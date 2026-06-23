package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ThirdPartyLoginDTO;
import com.atlas.auth.domain.dto.ThirdPartyStateContext;
import com.atlas.auth.domain.dto.ThirdPartyUserIdentity;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.TokenResponse;
import jakarta.annotation.Resource;

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
    protected ThirdPartyStateService thirdPartyStateService;

    protected final String generateState(ThirdPartyAuthAction action) {
        return thirdPartyStateService.generateState(getProviderName(),action,protocol());
    }

    protected final ThirdPartyStateContext validateState(String state) {
        return thirdPartyStateService.validateState(state,getProviderName());
    }

    protected TokenResponse dispatchFederatedIdentity(ThirdPartyUserIdentity thirdPartyUserIdentity, ThirdPartyStateContext stateContext){

        return switch (stateContext.getAction()) {
            case BIND -> doBind(thirdPartyUserIdentity, stateContext.getUserId());
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

}
