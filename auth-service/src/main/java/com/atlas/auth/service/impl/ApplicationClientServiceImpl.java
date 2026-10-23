package com.atlas.auth.service.impl;

import com.atlas.auth.config.security.oauth2.OAuth2ClientAuthorizedEvent;
import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.mapper.ApplicationClientMapper;
import com.atlas.auth.service.ApplicationClientService;
import com.atlas.common.mybatis.handler.DataPermissionContext;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/23 8:58
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationClientServiceImpl extends ServiceImpl<ApplicationClientMapper, ApplicationClient> implements ApplicationClientService {


    @Override
    public List<ApplicationClient> findByApplication(Long applicationId, ClientProtocol protocol) {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(protocol);
        return this.lambdaQuery()
                .eq(ApplicationClient::getApplicationId, applicationId)
                .eq(ApplicationClient::getProtocol, protocol)
                .orderByAsc(ApplicationClient::getId)
                .list();
    }

    @Override
    public PageInfo<ApplicationClient> getPage(Long applicationId, ClientProtocol protocol, Integer pageNum, Integer pageSize) {
        try (DataPermissionContext ctx = DataPermissionContext.open()) {
            PageHelper.startPage(pageNum, pageSize);
            List<ApplicationClient> applicationClients = this.lambdaQuery()
                    .eq(ApplicationClient::getApplicationId, applicationId)
                    .eq(ApplicationClient::getProtocol, protocol)
                    .orderByAsc(ApplicationClient::getCreateTime)
                    .orderByAsc(ApplicationClient::getId)
                    .list();
            if (CollectionUtils.isEmpty(applicationClients)) {
                return PageInfo.emptyPageInfo();
            }
            return new PageInfo<>(applicationClients);
        }
    }

    @Override
    public ApplicationClient findByRegisteredClientId(String registeredClientId) {
        Objects.requireNonNull(registeredClientId);
        return this.lambdaQuery()
                .eq(ApplicationClient::getRegisteredClientId, registeredClientId)
                .one();
    }

    @Override
    public ApplicationClient findByClientId(String clientId) {
        Objects.requireNonNull(clientId);
        return this.lambdaQuery()
                .eq(ApplicationClient::getClientId, clientId)
                .one();
    }


    // 监听客户端授权成功事件
    @Async
    @EventListener
    public void onClientAuthorized(OAuth2ClientAuthorizedEvent event) {
        String clientId = event.getClientId();
        LocalDateTime now = LocalDateTime.now();
        ApplicationClient applicationClient = this.findByClientId(clientId);
        if (applicationClient == null) {
            log.warn("该客户端关联的应用不存在: {}", clientId);
            return;
        }
        log.info("客户端授权成功，更新最后使用时间。clientId: {}", clientId);
        this.lambdaUpdate()
                .eq(ApplicationClient::getId, applicationClient.getId())
                .set(ApplicationClient::getLastUsedTime, now)
                .update();
    }
}
