package com.atlas.auth.config.security.cas;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.common.security.model.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/30 10:34
 */
@Slf4j
public class CasAuthenticationProvider implements AuthenticationProvider {

    private final CasService casService;

    private final CasRequestContextRepository casRequestContextRepository;

    public CasAuthenticationProvider(CasService casService, CasRequestContextRepository casRequestContextRepository) {
        this.casService = casService;
        this.casRequestContextRepository = casRequestContextRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CasAuthenticationToken casAuthenticationToken = (CasAuthenticationToken) authentication;

        CasRequestContext casRequestContext = casAuthenticationToken.getCasRequestContext();
        if(casRequestContext == null){
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST);
        }

        String service = casRequestContext.service();
        boolean renew = casRequestContext.renew();

        // 判断是否已认证
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = currentAuthentication != null
                && currentAuthentication.isAuthenticated()
                && !(currentAuthentication instanceof AnonymousAuthenticationToken);

        //  renew=true 时必须重新认证，不能复用已有登录态
        if (renew) {
            log.info("待实现");
        }

        // 用户未登录，保存请求上下文并进入登录流程
        if (!authenticated) {
            String casRequestId = casRequestContextRepository.save(casRequestContext);
            throw new CasAuthenticationRequiredException(casRequestId);
        }

        SecurityUser securityUser = (SecurityUser) currentAuthentication.getPrincipal();

        // 已登录 签发ticket
        String ticket = casService.issueServiceTicket(service, securityUser.getId());

        return new CasAuthenticationToken(securityUser, ticket, casRequestContext, currentAuthentication.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {

        return CasAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private CasAuthenticationException invalidRequest(String description) {

        return new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, description);
    }
}
