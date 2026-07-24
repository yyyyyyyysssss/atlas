package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ThirdPartyAuthRequestContext;
import com.atlas.auth.domain.dto.ThirdPartyLoginDTO;
import com.atlas.auth.domain.dto.ThirdPartyStateContext;
import com.atlas.auth.domain.dto.ThirdPartyUserIdentity;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.event.AuditLogEvent;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.TokenResponse;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;

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

    @Resource
    private ApplicationEventPublisher eventPublisher;

    protected final String generateState(ThirdPartyAuthRequestContext requestContext) {
        String state = requestContext.state();
        if(state != null && !state.isEmpty()){
            ThirdPartyStateContext thirdPartyStateContext = thirdPartyStateService.peekContext(state);
            if (thirdPartyStateContext == null){
                throw new BusinessException("授权状态已失效，请重试");
            }
            return state;
        }
        return thirdPartyStateService.generateState(getProviderName(),requestContext,protocol());
    }

    protected final ThirdPartyStateContext validateState(String state) {
        return thirdPartyStateService.validateState(state,getProviderName());
    }

    protected ThirdPartyCallbackVO dispatchFederatedIdentity(ThirdPartyUserIdentity thirdPartyUserIdentity){

        return dispatchFederatedIdentity(thirdPartyUserIdentity, null);
    }
    protected ThirdPartyCallbackVO dispatchFederatedIdentity(ThirdPartyUserIdentity thirdPartyUserIdentity, ThirdPartyStateContext stateContext){
        // 没上下文一律视为正常的、无状态登录流
        if (stateContext == null) {
            return doLogin(thirdPartyUserIdentity, null);
        }
        return switch (stateContext.getAction()) {
            case BIND -> doBind(thirdPartyUserIdentity, stateContext);
            default -> doLogin(thirdPartyUserIdentity, stateContext);
        };
    }

    private ThirdPartyCallbackVO doLogin(ThirdPartyUserIdentity thirdPartyUserIdentity, ThirdPartyStateContext stateContext){
        String provider = thirdPartyUserIdentity.getProvider();
        Long userId = userService.ensureUserByProvider(provider, thirdPartyUserIdentity);
        ThirdPartyLoginDTO thirdPartyLoginDTO = new ThirdPartyLoginDTO(ClientType.WEB, userId);
        TokenResponse tokenResponse = loginService.loginThirdParty(thirdPartyLoginDTO);
        String targetUrl = (stateContext != null) ? stateContext.getTargetUrl() : null;
        return ThirdPartyCallbackVO.loginSuccess(tokenResponse, targetUrl);
    }

    private ThirdPartyCallbackVO doBind(ThirdPartyUserIdentity thirdPartyUserIdentity, ThirdPartyStateContext stateContext){
        Long currentUserId = stateContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException("账号关联失败：未获取到当前账户登录凭证");
        }
        String provider = thirdPartyUserIdentity.getProvider();
        userService.bindThirdPartyProvider(provider,currentUserId, thirdPartyUserIdentity);

        // 绑定成功后，发布审计日志事件
        eventPublisher.publishEvent(new AuditLogEvent(currentUserId, "绑定三方账号", "user-provider"));

        return ThirdPartyCallbackVO.bindSuccess(stateContext.getTargetUrl());
    }

}
