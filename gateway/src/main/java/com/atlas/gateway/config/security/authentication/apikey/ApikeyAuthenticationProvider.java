package com.atlas.gateway.config.security.authentication.apikey;


import com.atlas.security.properties.SecurityProperties;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2024/9/10 10:00
 */
public class ApikeyAuthenticationProvider implements AuthenticationProvider {

    private final Map<String, List<String>> apikeyPathMap;

    private final static PathMatcher PATH_MATCHER = new AntPathMatcher();

    public final static String APIKEY_ROLE_CODE = "ROLE_APIKEY";

    private final static Set<? extends GrantedAuthority> REQUEST_HEADER_AUTHORITY = Collections.singleton((GrantedAuthority) () -> APIKEY_ROLE_CODE);

    public ApikeyAuthenticationProvider(List<SecurityProperties.RequestHeadAuthenticationConfig> requestHeadAuthentications) {
        if (requestHeadAuthentications == null || requestHeadAuthentications.isEmpty()) {
            throw new NullPointerException("requestHeadAuthentications not null");
        }
        this.apikeyPathMap = requestHeadAuthentications.stream().collect(Collectors.toMap(
                SecurityProperties.RequestHeadAuthenticationConfig::getApikey,
                c -> Arrays.asList(c.getPattern().split(","))
        ));
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String requestUrl = (String) authentication.getPrincipal();
        String apiKey = (String) authentication.getCredentials();
        List<String> allowedPatterns = apikeyPathMap.get(apiKey);
        if (allowedPatterns != null && allowedPatterns.stream().anyMatch(p -> PATH_MATCHER.match(p, requestUrl))) {
            return new PreAuthenticatedAuthenticationToken(requestUrl, null, REQUEST_HEADER_AUTHORITY);
        }
        throw new BadCredentialsException("API Key invalid or unauthorized for path: " + requestUrl);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(PreAuthenticatedAuthenticationToken.class);
    }
}
