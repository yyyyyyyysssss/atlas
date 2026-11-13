package com.atlas.common.core.aspect;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/11 11:41
 */
@Slf4j
@Aspect
public class ControllerLogAspect {

    private final ObjectMapper logObjectMapper;

    public ControllerLogAspect(ObjectMapper globalObjectMapper) {
        // 复制全局 ObjectMapper，避免修改全局序列化规则
        this.logObjectMapper = globalObjectMapper.copy();
        // 关键配置：即使遇到没法序列化的空
        this.logObjectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        SimpleModule module = new SimpleModule();
        // 局部注册：遇到以下类型直接忽略，不抛出异常
        module.addSerializer(ServletRequest.class, new ControllerLogAspect.LogTypeSerializer());
        module.addSerializer(ServletResponse.class, new ControllerLogAspect.LogTypeSerializer());
        module.addSerializer(MultipartFile.class, new ControllerLogAspect.LogTypeSerializer());
        module.addSerializer(InputStream.class, new ControllerLogAspect.LogTypeSerializer());
        module.addSerializer(Resource.class, new LogTypeSerializer());
        module.addSerializer(OutputStream.class, new LogTypeSerializer());
        module.addSerializer(Part.class, new LogTypeSerializer());
        module.addSerializer(byte[].class, new JsonSerializer<byte[]>() {
            @Override
            public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString("[Binary: " + value.length + " bytes]");
            }
        });
        this.logObjectMapper.registerModule(module);
    }

    @Pointcut("execution(* com.atlas..controller..*.*(..))")
    public void controllers() {
    }

    @Around("controllers()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        StringBuilder logBuilder = new StringBuilder();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        logBuilder.append(String.format("Request: [%s] %s", request.getMethod(), request.getRequestURI()));
        Object[] args = joinPoint.getArgs();
        logBuilder.append(String.format("   ,Args: %s",  logObjectMapper.writeValueAsString(toLogValue(args))));
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
                logBuilder.append(String.format("   ,Return: %s", logObjectMapper.writeValueAsString(toLogValue(result))));
                log.info(logBuilder.toString());
            }
        }
    }

    public static class LogTypeSerializer extends JsonSerializer<Object> {

        @Override
        public void serialize(Object object, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString("[Object: " + object.getClass().getSimpleName() + "]");
        }
    }

    private Object toLogValue(Object value) {
        if (value == null) {
            return null;
        }

        String className = value.getClass().getName();

        if (className.equals("org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody")) {
            return "[StreamingResponseBody]";
        }

        return value;
    }


}
