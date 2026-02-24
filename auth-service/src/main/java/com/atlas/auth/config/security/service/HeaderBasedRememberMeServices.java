package com.atlas.auth.config.security.service;

import com.atlas.security.enums.TokenType;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/24 11:56
 */
public class HeaderBasedRememberMeServices implements RememberMeServices {

    private String key;

    private TokenService tokenService;

    private UserDetailsService userDetailsService;

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();

    private static final Pattern authorizationPattern = Pattern.compile("^remember-me (?<token>[a-zA-Z0-9-._~+/]+=*)$", 2);

    private String bearerTokenHeaderName = "x-remember-me";

    public HeaderBasedRememberMeServices(String key, TokenService tokenService,UserDetailsService userDetailsService){
        this.key = key;
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
        String token = resolveToken(request);
        if(token == null){
            return null;
        }
        PayloadInfo payloadInfo = tokenService.verify(token, TokenType.REMEMBER_ME_TOKEN);
        String username = payloadInfo.getSubject();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return createSuccessfulAuthentication(request,userDetails);
    }

    @Override
    public void loginFail(HttpServletRequest request, HttpServletResponse response) {

    }

    @Override
    public void loginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {

    }

    protected Authentication createSuccessfulAuthentication(HttpServletRequest request, UserDetails user) {
        RememberMeAuthenticationToken auth = new RememberMeAuthenticationToken(this.key, user, user.getAuthorities());
        auth.setDetails(this.authenticationDetailsSource.buildDetails(request));
        return auth;
    }

    private String resolveToken(HttpServletRequest request){
        String authorization = request.getHeader(this.bearerTokenHeaderName);
        if (!StringUtils.startsWithIgnoreCase(authorization, "remember-me")) {
            return null;
        } else {
            Matcher matcher = authorizationPattern.matcher(authorization);
            if (!matcher.matches()) {
                throw new BadCredentialsException("remember-me token is malformed");
            }
            return matcher.group("token");
        }
    }
}
