package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.ApplicationClientSaveDTO;
import com.atlas.auth.domain.dto.CasClientSaveDTO;
import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.domain.entity.CasRegisteredClient;
import com.atlas.auth.domain.vo.CasClientCreateVO;
import com.atlas.auth.domain.vo.CasClientVO;
import com.atlas.auth.event.AuditLogEvent;
import com.atlas.common.core.context.UserContext;
import com.atlas.common.crypto.digest.DigestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/12 10:31
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class CasClientFacadeService extends AbstractClientApplicationFacadeService{

    private final CasRegisteredClientService casRegisteredClientService;

    private final ApplicationClientService applicationClientService;

    private final ApplicationEventPublisher eventPublisher;

    private static final String AUDIT_LOG_TARGET = "project:application:client:cas";

    public CasClientCreateVO save(String projectCode, Long applicationId, CasClientSaveDTO saveDTO) {
        validateProjectAndApplication(projectCode, applicationId);
        CasClientCreateVO vo = null;
        if (saveDTO.id() == null) {
            vo = createClient(applicationId, saveDTO);
        } else {
            updateClient(applicationId, saveDTO);
        }
        return vo;
    }

    public CasClientVO getDetail(String projectCode, Long applicationId, Long id) {
        // 前置校验
        validateProjectAndApplication(projectCode, applicationId);

        ApplicationClient applicationClient = applicationClientService.getById(id);
        if (applicationClient == null) {
            throw new AuthServiceException("Cas客户端不存在");
        }
        CasRegisteredClient registeredClient = casRegisteredClientService.getById(applicationClient.getRegisteredClientId());
        if (registeredClient == null) {
            throw new AuthServiceException("应用关联的Cas客户端数据丢失，请检查数据一致性");
        }
        return CasClientVO.of(applicationClient, registeredClient);
    }

    @Transactional
    public void deleteById(String projectCode,Long applicationId, Long id) {
        validateProjectAndApplication(projectCode, applicationId);

        log.info("正在删除 Cas 客户端，ID: {}", id);
        ApplicationClient applicationClient = applicationClientService.getById(id);
        if (applicationClient == null) {
            throw new AuthServiceException("Cas客户端不存在");
        }

        // 删除客户端应用主表信息
        boolean delFlag = applicationClientService.removeById(id);
        if (!delFlag) {
            log.error("删除 Cas 客户端主表记录失败，ID: {}", id);
            throw new AuthServiceException("删除Cas 客户端失败，请稍后重试");
        }

        // 删除客户端注册信息
        casRegisteredClientService.removeById(applicationClient.getRegisteredClientId());

        log.info("Cas 客户端删除成功，ID: {}", id);

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "删除Cas客户端", AUDIT_LOG_TARGET));
    }


    private CasClientCreateVO createClient(Long applicationId, CasClientSaveDTO saveDTO) {
        log.info("正在创建 Cas 应用，名称: {}", saveDTO.clientName());
        if (Boolean.TRUE.equals(saveDTO.proxyEnabled()) && CollectionUtils.isEmpty(saveDTO.proxyCallbackUrls())) {
            throw new AuthServiceException("开启代理认证时，代理票据回调地址不能为空");
        }
        String registeredClientId = UUID.randomUUID().toString().replace("-", "");

        CasRegisteredClient casRegisteredClient = CasRegisteredClient.builder()
                .id(registeredClientId)
                .clientName(saveDTO.clientName())
                .service(saveDTO.service())
                .serviceHash(DigestUtils.sha256(saveDTO.service()))
                .proxyEnabled(saveDTO.proxyEnabled() != null ? saveDTO.proxyEnabled() : false)
                .proxyCallbackUrls(saveDTO.proxyCallbackUrls())
                .attributeMappings(saveDTO.attributeMappings())
                .build();

        String clientId = casRegisteredClient.getServiceHash();

        casRegisteredClientService.save(casRegisteredClient);

        // 保存 (application_client)
        ApplicationClientSaveDTO clientSaveDTO = ApplicationClientSaveDTO.of(applicationId, saveDTO, casRegisteredClient);
        ApplicationClient applicationClient = applicationClientService.saveInfo(clientSaveDTO);

        log.info("Cas 客户端创建成功, id: {}, clientId: {}", applicationClient.getId(), clientId);

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "创建Cas客户端", AUDIT_LOG_TARGET));

        return new CasClientCreateVO(applicationClient.getId(), clientId);
    }

    private void updateClient(Long applicationId, CasClientSaveDTO saveDTO) {
        log.info("正在修改 Cas 客户端，ID: {}", saveDTO.id());

        if (Boolean.TRUE.equals(saveDTO.proxyEnabled()) && CollectionUtils.isEmpty(saveDTO.proxyCallbackUrls())) {
            throw new AuthServiceException("开启代理认证时，代理票据回调地址不能为空");
        }

        ApplicationClient applicationClient = applicationClientService.getById(saveDTO.id());
        if(applicationClient == null){
            throw new AuthServiceException("客户端不存在");
        }
        CasRegisteredClient casRegisteredClient = casRegisteredClientService.getById(applicationClient.getRegisteredClientId());
        if (casRegisteredClient == null) {
            throw new AuthServiceException("客户端[" + applicationClient.getClientName() + "]关联的Cas配置数据丢失，请检查数据一致性");
        }
        if(StringUtils.hasText(saveDTO.clientName())){
            casRegisteredClient.setClientName(saveDTO.clientName());
        }
        if(StringUtils.hasText(saveDTO.service())){
            casRegisteredClient.setService(saveDTO.service());
            casRegisteredClient.setServiceHash(DigestUtils.sha256(saveDTO.service()));
        }
        if(saveDTO.proxyEnabled() != null){
            casRegisteredClient.setProxyEnabled(saveDTO.proxyEnabled());
        }
        if(saveDTO.proxyEnabled() != null){
            casRegisteredClient.setProxyEnabled(saveDTO.proxyEnabled());
            casRegisteredClient.setProxyCallbackUrls(saveDTO.proxyCallbackUrls());
        }
        if(saveDTO.attributeMappings() != null && !saveDTO.attributeMappings().isEmpty()){
            casRegisteredClient.setAttributeMappings(saveDTO.attributeMappings());
        }
        casRegisteredClientService.updateById(casRegisteredClient);

        ApplicationClientSaveDTO clientSaveDTO = ApplicationClientSaveDTO.of(applicationId, saveDTO, casRegisteredClient);
        applicationClientService.saveInfo(clientSaveDTO);

        log.info("Cas 客户端配置更新成功, clientName: {}", applicationClient.getClientName());

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "修改Cas客户端", AUDIT_LOG_TARGET));
    }

}
