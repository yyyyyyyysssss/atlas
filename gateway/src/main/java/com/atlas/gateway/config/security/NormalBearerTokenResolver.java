package com.atlas.gateway.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 10:42
 */
public class NormalBearerTokenResolver {

    private static final Pattern authorizationPattern = Pattern.compile("^Bearer (?<token>[a-zA-Z0-9-._~+/]+=*)$", 2);

    private String bearerTokenHeaderName = "Authorization";


    public String resolve(HttpServletRequest request) {
        return resolveFromAuthorizationHeader(request);
    }

    private String resolveFromAuthorizationHeader(HttpServletRequest request) {
        String authorization = request.getHeader(this.bearerTokenHeaderName);
        if(authorization == null || authorization.isEmpty()){
            return request.getParameter("access_token");
        }
        if (!StringUtils.startsWithIgnoreCase(authorization, "Bearer")) {
            return null;
        } else {
            Matcher matcher = authorizationPattern.matcher(authorization);
            if (!matcher.matches()) {

                throw new BadCredentialsException("Bearer token is malformed");
            } else {
                return matcher.group("token");
            }
        }
    }

}
