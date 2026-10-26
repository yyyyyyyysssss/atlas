package com.atlas.auth.service.impl;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.ApplicationClientSaveDTO;
import com.atlas.auth.domain.entity.Application;
import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.domain.vo.ApplicationClientVO;
import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.event.ClientAuthorizedEvent;
import com.atlas.auth.mapper.ApplicationClientMapper;
import com.atlas.auth.service.ApplicationClientService;
import com.atlas.auth.service.ApplicationService;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.mybatis.handler.DataPermissionContext;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
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

    @Resource
    private ApplicationService applicationService;

    @Override
    public ApplicationClient saveInfo(ApplicationClientSaveDTO saveDTO) {
        ApplicationClient applicationClient;
        if (saveDTO.id() == null) {
            applicationClient = ApplicationClient
                    .builder()
                    .applicationId(saveDTO.applicationId())
                    .registeredClientId(saveDTO.registeredClientId())
                    .clientId(saveDTO.clientId())
                    .clientName(saveDTO.clientName())
                    .protocol(saveDTO.clientProtocol())
                    .description(saveDTO.description())
                    .build();
            applicationClient.setId(IdGen.genId());
            this.saveOrUpdate(applicationClient);
        } else {
            applicationClient = this.getById(saveDTO.id());
            if (applicationClient == null) {
                throw new AuthServiceException("应用客户端不存在");
            }
            if(saveDTO.clientProtocol().equals(ClientProtocol.SAML2) && StringUtils.hasText(saveDTO.clientId())){
                applicationClient.setClientId(saveDTO.clientId());
            }
            applicationClient.setClientName(saveDTO.clientName());
            applicationClient.setDescription(saveDTO.description());
            this.updateById(applicationClient);
        }
        return applicationClient;
    }

    @Override
    public PageInfo<ApplicationClientVO> getPage(Long applicationId, ClientProtocol protocol, Integer pageNum, Integer pageSize) {
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
            return new PageInfo<>(applicationClients).convert(ApplicationClientVO::of);
        }
    }

    @Override
    public ApplicationClient getActiveByRegisteredClientId(String registeredClientId, ClientProtocol protocol) {
        ApplicationClient applicationClient = this.lambdaQuery()
                .eq(ApplicationClient::getRegisteredClientId, registeredClientId)
                .eq(ApplicationClient::getProtocol, protocol)
                .one();
        if (applicationClient == null) {
            return null;
        }
        Application application = applicationService.getActive(applicationClient.getApplicationId());
        if (application == null) {
            return null;
        }
        return applicationClient;
    }

    @Override
    public ApplicationClient findByClientId(String clientId, ClientProtocol protocol) {
        Objects.requireNonNull(clientId);
        return this.lambdaQuery()
                .eq(ApplicationClient::getClientId, clientId)
                .eq(ApplicationClient::getProtocol, protocol)
                .one();
    }

    @Override
    public List<ApplicationClient> findByApplicationIds(Collection<Long> applicationIds) {
        List<ApplicationClient> list = this.lambdaQuery()
                .in(ApplicationClient::getApplicationId, applicationIds)
                .list();
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    // 监听客户端授权成功事件
    @Async
    @EventListener
    public void onClientAuthorized(ClientAuthorizedEvent event) {
        String clientId = event.getClientId();
        ClientProtocol protocol = event.getProtocol();
        LocalDateTime now = LocalDateTime.now();
        ApplicationClient applicationClient = this.findByClientId(clientId, protocol);
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
