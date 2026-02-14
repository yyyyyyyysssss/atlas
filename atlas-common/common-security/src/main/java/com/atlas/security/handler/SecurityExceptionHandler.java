package com.atlas.security.handler;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultCode;
import com.atlas.common.core.response.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 14:13
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class SecurityExceptionHandler {


    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({AuthenticationException.class})
    public Result<?> handlerAuthenticationException(AuthenticationException authenticationException){
        if(authenticationException instanceof BadCredentialsException
                || authenticationException instanceof UsernameNotFoundException
        ){
            log.error("用户名或密码错误: ",authenticationException);
            return ResultGenerator.failed(ResultCode.AUTH_LOGIN_FAILED);
        }
        log.error("认证异常: ",authenticationException);
        return ResultGenerator.failed(ResultCode.UNAUTHORIZED);
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

}
