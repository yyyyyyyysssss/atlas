package com.atlas.security.exception;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultCode;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.properties.SecurityProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 14:13
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class SecurityExceptionAdvice {

    @Resource
    private SecurityProperties securityProperties;

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({AuthenticationException.class})
    public Result<?> handlerAuthenticationException(AuthenticationException authenticationException){
        if(authenticationException instanceof BadCredentialsException || authenticationException instanceof UsernameNotFoundException){
            log.error("用户名或密码错误: {}",authenticationException.getMessage());
            return ResultGenerator.failed(ResultCode.AUTH_LOGIN_FAILED,authenticationException.getMessage());
        }
        log.error("认证异常: ",authenticationException);
        return ResultGenerator.failed(ResultCode.UNAUTHORIZED,authenticationException.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({AccountStatusException.class})
    public Result<?> handlerAccountStatusException(AccountStatusException accountStatusException){
        if(accountStatusException instanceof LockedException){
            log.error("账号已锁定: ",accountStatusException);
            return ResultGenerator.failed(ResultCode.AUTH_ACCOUNT_LOCKED);
        }
        if(accountStatusException instanceof DisabledException){
            log.error("账号已停用: ",accountStatusException);
            return ResultGenerator.failed(ResultCode.AUTH_ACCOUNT_DISABLED);
        }
        log.error("账号状态异常: ",accountStatusException);
        return ResultGenerator.failed(ResultCode.AUTH_ACCOUNT_LOCKED);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler({AccessDeniedException.class})
    public Result<?> handlerAccessDeniedException(AccessDeniedException accessDeniedException){
        log.error("Access Denied: ",accessDeniedException);
        return ResultGenerator.failed(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler({OAuth2AuthenticationException.class})
    public RedirectView handlerOAuth2AuthenticationException(OAuth2AuthenticationException e){
        log.error("OAuth2 认证异常, 错误码: {}, 原因: {}",
                e.getError().getErrorCode(), e.getMessage());
        OAuth2Error error = e.getError();
        String errorCode = (error != null) ? error.getErrorCode() : "invalid_request";
        String message = (error != null && StringUtils.hasText(error.getDescription()))
                ? error.getDescription()
                : e.getMessage();
        String errorUri = (error != null) ? error.getUri() : null;

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(securityProperties.getUiUrl())
                .path("/500")
                .queryParam("error", errorCode)
                .queryParam("message", message);

        if (StringUtils.hasText(errorUri)) {
            builder.queryParam("errorUri", errorUri);
        }
        return new RedirectView(builder.build().encode().toUriString());
    }

}
