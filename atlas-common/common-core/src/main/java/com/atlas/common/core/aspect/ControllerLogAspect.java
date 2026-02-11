package com.atlas.common.core.aspect;

import com.atlas.common.core.json.mixin.JacksonIgnoreMixIn;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/11 11:41
 */
@Slf4j
public class ControllerLogAspect {

    private final ObjectMapper logObjectMapper;

    public ControllerLogAspect(ObjectMapper globalObjectMapper) {
        // 复制全局 ObjectMapper，避免修改全局序列化规则
        this.logObjectMapper = globalObjectMapper.copy();
        // 局部注册：遇到以下类型直接忽略，不抛出异常
        this.logObjectMapper.addMixIn(HttpServletRequest.class, JacksonIgnoreMixIn.class);
        this.logObjectMapper.addMixIn(HttpServletResponse.class, JacksonIgnoreMixIn.class);
        this.logObjectMapper.addMixIn(MultipartFile.class, JacksonIgnoreMixIn.class);
        this.logObjectMapper.addMixIn(InputStream.class, JacksonIgnoreMixIn.class);
    }

    @Pointcut("execution(* com.atlas..controller..*.*(..))")
    public void controllers() {
    }

    @Around("controllerPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        StringBuilder logBuilder = new StringBuilder();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        logBuilder.append(String.format("Request: [%s] %s", request.getMethod(), request.getRequestURI()));
        logBuilder.append(String.format("   ,Args: %s",  logObjectMapper.writeValueAsString(joinPoint.getArgs())));
        Object result = null;
        Throwable exception = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        }finally {
            long diff = System.currentTimeMillis() - startTime;
            logBuilder.append(String.format("   Spend: %f s", diff / 1000.0));
            if(exception != null){
                logBuilder.append(String.format("   ,Exception: %s", exception.getMessage()));
                log.error(logBuilder.toString());
            } else {
                logBuilder.append(String.format("   ,Return: %s", logObjectMapper.writeValueAsString(result)));
                log.info(logBuilder.toString());
            }
        }
    }

}
