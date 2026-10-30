package com.atlas.auth.config.security.cas;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.auth.domain.entity.CasRegisteredClient;
import com.atlas.auth.service.CasRegisteredClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/30 10:34
 */
@Slf4j
public class CasRequestAuthenticationProvider implements AuthenticationProvider {

    private final CasRegisteredClientService casRegisteredClientService;

    public CasRequestAuthenticationProvider(CasRegisteredClientService casRegisteredClientService) {
        this.casRegisteredClientService = casRegisteredClientService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CasRequestAuthenticationToken requestAuthenticationToken = (CasRequestAuthenticationToken) authentication;
        String service = requestAuthenticationToken.getService();
        boolean renew = requestAuthenticationToken.isRenew();
        boolean gateway = requestAuthenticationToken.isGateway();
        String relayState = requestAuthenticationToken.getRelayState();

        // 校验 Service 参数
        if (service == null || service.isBlank()) {
            throw invalidRequest("Missing service parameter");
        }

        // 查询已注册且启用的 CAS 客户端
        CasRegisteredClient registeredClient = casRegisteredClientService.getEnabledByService(service);
        if (registeredClient == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_SERVICE, service);
        }

        CasRequestContext casRequestContext = new CasRequestContext(service, renew, gateway, relayState);

        return new CasAuthenticationToken(casRequestContext);
    }

    @Override
    public boolean supports(Class<?> authentication) {

        return CasRequestAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private CasAuthenticationException invalidRequest(String description) {

        return new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, description);
    }
}
