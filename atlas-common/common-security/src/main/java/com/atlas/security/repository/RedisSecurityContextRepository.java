package com.atlas.security.repository;

import com.atlas.common.core.constant.CommonConstant;
import com.atlas.security.properties.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2024/7/17 15:16
 */
@Slf4j
public class RedisSecurityContextRepository implements SecurityContextStore {

    public static final String DEFAULT_REQUEST_ATTR_NAME = "ATLAS_SECURITY_CONTEXT";

    private static final String SECURITY_CONTEXT_KEY_PREFIX = "security:context:repository:";

    private RedisTemplate<String,Object> redisTemplate;

    private SecurityProperties securityProperties;

    public RedisSecurityContextRepository(RedisTemplate<String,Object> redisTemplate, SecurityProperties securityProperties){
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        HttpServletRequest request = requestResponseHolder.getRequest();
        String tokenId = resolveTokenId(request);
        if (tokenId == null || tokenId.isEmpty()){
            return null;
        }
        try {
            return (SecurityContext)redisTemplate.opsForValue().get(SECURITY_CONTEXT_KEY_PREFIX + tokenId);
        } catch (Exception e) {
            log.error("从Redis加载SecurityContext失败, tokenId: {}", tokenId, e);
            return null;
        }
    }

    @Override
    public void saveContext(SecurityContext context, String tokenId) {
        if (tokenId == null || tokenId.isEmpty()) {
            return;
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        HttpServletResponse response = ((ServletRequestAttributes) requestAttributes).getResponse();
        request.setAttribute(DEFAULT_REQUEST_ATTR_NAME, tokenId);
        saveContext(context,request,response);
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        String tokenId = resolveTokenId(request);
        if (tokenId == null || tokenId.isEmpty()){
            return;
        }
        try {
            // 如果当前的context是空的，则移除
            SecurityContext emptyContext = this.securityContextHolderStrategy.createEmptyContext();
            if (emptyContext.equals(context)){
                redisTemplate.delete(SECURITY_CONTEXT_KEY_PREFIX + tokenId);
            }else {
                Long expiration = securityProperties.getJwt().getExpiration();
                redisTemplate.opsForValue().set(SECURITY_CONTEXT_KEY_PREFIX + tokenId,context, Duration.ofSeconds(expiration));
            }
        }finally {
            request.removeAttribute(DEFAULT_REQUEST_ATTR_NAME);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        String tokenId = resolveTokenId(request);
        if (tokenId == null || tokenId.isEmpty()){
            return false;
        }
        return containsContext(tokenId);
    }


    @Override
    public boolean clearContext(String tokenId){
        if (tokenId == null || tokenId.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.delete(SECURITY_CONTEXT_KEY_PREFIX + tokenId));
    }

    @Override
    public boolean containsContext(String tokenId) {
        if (tokenId == null || tokenId.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(SECURITY_CONTEXT_KEY_PREFIX + tokenId));
    }

    private String resolveTokenId(HttpServletRequest request){
        String tokenId = (String)request.getAttribute(DEFAULT_REQUEST_ATTR_NAME);
        if(tokenId == null || tokenId.isEmpty()){
            tokenId = request.getHeader(CommonConstant.TOKEN_ID);
        }
        return tokenId;
    }
}
