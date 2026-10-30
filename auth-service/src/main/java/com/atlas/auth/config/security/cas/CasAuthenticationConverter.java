package com.atlas.auth.config.security.cas;

import com.atlas.auth.config.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/10 17:47
 */
public class CasAuthenticationConverter implements AuthenticationConverter {

    private CasRequestContextRepository casRequestContextRepository;

    public CasAuthenticationConverter(CasRequestContextRepository casRequestContextRepository){
        this.casRequestContextRepository = casRequestContextRepository;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String casRequestId = request.getParameter("casRequestId");
        if(casRequestId == null || casRequestId.isBlank()){
            return null;
        }
        CasRequestContext casRequestContext = casRequestContextRepository.consume(casRequestId);
        if(casRequestContext == null){
            throw invalidRequest("CAS authentication context expired or invalid");
        }
        return new CasAuthenticationToken(casRequestContext);
    }

    public static CasAuthenticationException invalidRequest(String description) {

        return new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, description);
    }
}
