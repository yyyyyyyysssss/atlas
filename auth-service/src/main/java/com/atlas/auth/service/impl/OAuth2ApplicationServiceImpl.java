package com.atlas.auth.service.impl;

import com.atlas.auth.config.security.oauth2.OAuth2ClientAuthorizedEvent;
import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.enums.ProjectStatus;
import com.atlas.auth.mapper.OAuth2ApplicationMapper;
import com.atlas.auth.service.OAuth2ClientApplicationService;
import com.atlas.auth.service.ProjectService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2ApplicationServiceImpl extends ServiceImpl<OAuth2ApplicationMapper, OAuth2ClientApplication> implements OAuth2ClientApplicationService {


    private final ProjectService projectService;

    @Override
    public OAuth2ClientApplication loadClientByRegisteredClientId(String registeredClientId) {
        OAuth2ClientApplication application = this.lambdaQuery()
                .eq(OAuth2ClientApplication::getRegisteredClientId, registeredClientId)
                .one();
        return loadClient(application);
    }

    @Override
    public OAuth2ClientApplication loadClientByClientId(String clientId) {
        OAuth2ClientApplication application = this.lambdaQuery()
                .eq(OAuth2ClientApplication::getClientId, clientId)
                .one();
        return loadClient(application);
    }

    private OAuth2ClientApplication loadClient(OAuth2ClientApplication application){
        if(application == null){
            log.warn("OAuth2 客户端认证失败：应用不存在");
            throw new BadCredentialsException("Invalid client credentials");
        }
        Long projectId = application.getProjectId();
        Project project = projectService.getById(projectId);
        if(project == null){
            log.warn("OAuth2 客户端认证失败：关联项目不存在，projectId: {}", projectId);
            throw new BadCredentialsException("Invalid client credentials");
        }
        if (!ProjectStatus.ACTIVE.equals(project.getStatus())) {
            log.warn("OAuth2 客户端认证失败：项目未激活/已停用，projectId: {}, status: {}", projectId, project.getStatus());
            throw new BadCredentialsException("Client project is suspended or inactive");
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
