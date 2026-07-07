package com.atlas.auth.service.impl;

import com.atlas.auth.config.security.oauth2.OAuth2ClientAuthorizedEvent;
import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.atlas.auth.mapper.OAuth2ApplicationMapper;
import com.atlas.auth.service.OAuth2ClientApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@AllArgsConstructor
@Slf4j
public class OAuth2ApplicationServiceImpl extends ServiceImpl<OAuth2ApplicationMapper, OAuth2ClientApplication> implements OAuth2ClientApplicationService {


    @Override
    public OAuth2ClientApplication getByRegisteredClientId(String registeredClientId) {
        return this.lambdaQuery()
                .eq(OAuth2ClientApplication::getRegisteredClientId, registeredClientId)
                .one();
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
