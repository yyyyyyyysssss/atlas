package com.atlas.auth.service.impl;

import com.atlas.auth.config.security.oauth2.OAuth2ClientAuthorizedEvent;
import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.atlas.auth.mapper.OAuth2ApplicationMapper;
import com.atlas.auth.service.OAuth2ClientApplicationService;
import com.atlas.auth.service.ProjectService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@Slf4j
public class OAuth2ApplicationServiceImpl extends ServiceImpl<OAuth2ApplicationMapper, OAuth2ClientApplication> implements OAuth2ClientApplicationService {


    @Resource
    @Lazy
    private ProjectService projectService;

    @Override
    public OAuth2ClientApplication loadClientByRegisteredClientId(String registeredClientId) {
        OAuth2ClientApplication application = findByRegisteredClientId(registeredClientId);
        return loadClient(application);
    }

    @Override
    public OAuth2ClientApplication loadClientByClientId(String clientId) {
        OAuth2ClientApplication application = findByClientId(clientId);
        return loadClient(application);
    }

    @Override
    public OAuth2ClientApplication findByRegisteredClientId(String registeredClientId){
        return this.lambdaQuery()
                .eq(OAuth2ClientApplication::getRegisteredClientId, registeredClientId)
                .one();
    }

    @Override
    public OAuth2ClientApplication findByClientId(String clientId){
        return this.lambdaQuery()
                .eq(OAuth2ClientApplication::getClientId, clientId)
                .one();
    }

    private OAuth2ClientApplication loadClient(OAuth2ClientApplication application){
        if(application == null){
            log.warn("OAuth2 客户端认证失败：应用不存在");
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "客户端应用不存在", null)
            );
        }
        Long projectId = application.getProjectId();
        if(!projectService.isProjectActive(projectId)){
            log.warn("OAuth2 客户端认证失败：项目不存在或未激活，projectId: {}", projectId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(
                            OAuth2ErrorCodes.INVALID_CLIENT,
                            "客户端所属项目已被禁用或停用",
                            null
                    )
            );
        }
        return application;
    }

    // 监听 OAuth2 客户端授权成功事件
    @Async
    @EventListener
    public void onClientAuthorized(OAuth2ClientAuthorizedEvent event) {
        String clientId = event.getClientId();
        LocalDateTime now = LocalDateTime.now();
        log.info("OAuth2 客户端授权成功，更新最后使用时间。clientId: {}", clientId);
        this.lambdaUpdate()
                .eq(OAuth2ClientApplication::getClientId, clientId)
                .set(OAuth2ClientApplication::getLastUsedTime, now)
                .update();
    }


}
