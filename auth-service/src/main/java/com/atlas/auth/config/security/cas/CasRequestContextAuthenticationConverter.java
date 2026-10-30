package com.atlas.auth.config.security.cas;

import com.atlas.auth.config.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 10:37
 */
public class CasRequestContextAuthenticationConverter implements AuthenticationConverter {

    private static final String PARAM_SERVICE = "service";
    private static final String PARAM_RENEW = "renew";
    private static final String PARAM_GATEWAY = "gateway";

    @Override
    public Authentication convert(HttpServletRequest request) {
        String service = getSingleParameter(request, PARAM_SERVICE);
        String renew = getSingleParameter(request, PARAM_RENEW);
        String gateway = getSingleParameter(request, PARAM_GATEWAY);
        // renew 和 gateway 只能是 true
        boolean renewFlag = parseBooleanParameter(PARAM_RENEW, renew);
        boolean gatewayFlag = parseBooleanParameter(PARAM_GATEWAY, gateway);

        // renew 和 gateway 不能同时使用
        if (renewFlag && gatewayFlag) {
            throw invalidRequest("renew and gateway cannot be used together");
        }

        if (service != null && !StringUtils.hasText(service)) {
            throw invalidRequest("Invalid service parameter");
        }
        return new CasRequestAuthenticationToken(
                service,
                renewFlag,
                gatewayFlag,
                request.getParameter("relayState")
        );
    }

    private String getSingleParameter(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length == 0) {
            return null;
        }
        if (values.length != 1) {
            throw invalidRequest("Duplicate " + name + " parameter");
        }

        return values[0];
    }

    private boolean parseBooleanParameter(String name, String value) {

        if (value == null) {
            return false;
        }

        if ("true".equalsIgnoreCase(value)) {
            return true;
        }

        if ("false".equalsIgnoreCase(value)) {
            return false;
        }

        throw invalidRequest("Invalid " + name + " parameter");
    }

    public static CasAuthenticationException invalidRequest(String description) {

        return new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, description);
    }
}
