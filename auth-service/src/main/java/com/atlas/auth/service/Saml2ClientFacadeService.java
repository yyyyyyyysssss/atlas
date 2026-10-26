package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.ApplicationClientSaveDTO;
import com.atlas.auth.domain.dto.Saml2ClientSaveDTO;
import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.atlas.auth.domain.vo.Saml2ClientCreateVO;
import com.atlas.auth.domain.vo.Saml2ClientVO;
import com.atlas.auth.event.AuditLogEvent;
import com.atlas.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/22 16:21
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class Saml2ClientFacadeService extends AbstractClientApplicationFacadeService {

    private final Saml2RegisteredClientService saml2RegisteredClientService;

    private final ApplicationClientService applicationClientService;

    private final ApplicationEventPublisher eventPublisher;

    private static final String AUDIT_LOG_TARGET = "project:application:client:saml2";

    public Saml2ClientCreateVO save(String projectCode, Long applicationId, Saml2ClientSaveDTO saveDTO) {
        validateProjectAndApplication(projectCode, applicationId);

        Saml2ClientCreateVO vo = null;
        if (saveDTO.id() == null) {
            vo = createClient(applicationId, saveDTO);
        } else {
            updateClient(applicationId, saveDTO);
        }
        return vo;
    }

    public Saml2ClientVO getDetail(String projectCode, Long applicationId, Long id) {
        // 前置校验
        validateProjectAndApplication(projectCode, applicationId);

        ApplicationClient applicationClient = applicationClientService.getById(id);
        if (applicationClient == null) {
            throw new AuthServiceException("saml2客户端不存在");
        }
        Saml2RegisteredClient registeredClient = saml2RegisteredClientService.getById(applicationClient.getRegisteredClientId());
        if (registeredClient == null) {
            throw new AuthServiceException("应用关联的saml2客户端数据丢失，请检查数据一致性");
        }
        return Saml2ClientVO.of(applicationClient, registeredClient);
    }

    @Transactional
    public void deleteById(String projectCode,Long applicationId, Long id) {
        validateProjectAndApplication(projectCode, applicationId);

        log.info("正在删除 Saml2 客户端，ID: {}", id);
        ApplicationClient applicationClient = applicationClientService.getById(id);
        if (applicationClient == null) {
            throw new AuthServiceException("saml2客户端不存在");
        }

        // 删除客户端应用主表信息
        boolean delFlag = applicationClientService.removeById(id);
        if (!delFlag) {
            log.error("删除 Saml2 客户端主表记录失败，ID: {}", id);
            throw new AuthServiceException("删除Saml2 客户端失败，请稍后重试");
        }

        log.info("Saml2 客户端删除成功，ID: {}", id);

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "删除Saml2客户端", AUDIT_LOG_TARGET));
    }

    private Saml2ClientCreateVO createClient(Long applicationId, Saml2ClientSaveDTO saveDTO) {
        log.info("正在创建 Saml2 应用，名称: {}", saveDTO.clientName());

        String registeredClientId = UUID.randomUUID().toString().replace("-", "");
        if (Boolean.TRUE.equals(saveDTO.requireSignedAuthnRequest()) && !StringUtils.hasText(saveDTO.verificationCertificate())) {
            throw new AuthServiceException("AuthnRequest 必须签名时, 请求签名验证证书不能为空");
        }
        boolean requireSignedAuthnRequest = saveDTO.requireSignedAuthnRequest() != null ? saveDTO.requireSignedAuthnRequest() : false;
        if (StringUtils.hasText(saveDTO.verificationCertificate())) {
            requireSignedAuthnRequest = true;
        }
        boolean signResponse = saveDTO.signResponse() != null ? saveDTO.signResponse() : true;
        boolean signAssertion = saveDTO.signAssertion() != null ? saveDTO.signAssertion() : true;

        Saml2RegisteredClient saml2RegisteredClient = Saml2RegisteredClient.builder()
                .id(registeredClientId)
                .entityId(saveDTO.entityId())
                .clientName(saveDTO.clientName())
                .acsUrl(saveDTO.acsUrl())
                .requireSignedAuthnRequest(requireSignedAuthnRequest)
                .verificationCertificate(saveDTO.verificationCertificate())
                .signResponse(signResponse)
                .signAssertion(signAssertion)
                .attributeMappings(saveDTO.attributeMappings())
                .build();

        String clientId = saveDTO.entityId();

        saml2RegisteredClientService.save(saml2RegisteredClient);

        // 保存 (application_client)
        ApplicationClientSaveDTO clientSaveDTO = ApplicationClientSaveDTO.of(applicationId, saveDTO, saml2RegisteredClient);
        ApplicationClient applicationClient = applicationClientService.saveInfo(clientSaveDTO);

        log.info("Saml2 客户端创建成功, id: {}, clientId: {}", applicationClient.getId(), clientId);

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "创建Saml2客户端", AUDIT_LOG_TARGET));

        return new Saml2ClientCreateVO(applicationClient.getId(), clientId);
    }

    private void updateClient(Long applicationId, Saml2ClientSaveDTO saveDTO) {
        log.info("正在修改 Saml2 客户端，ID: {}", saveDTO.id());

        ApplicationClient applicationClient = applicationClientService.getById(saveDTO.id());
        if(applicationClient == null){
            throw new AuthServiceException("客户端不存在");
        }

        Saml2RegisteredClient saml2RegisteredClient = saml2RegisteredClientService.getById(applicationClient.getRegisteredClientId());
        if (saml2RegisteredClient == null) {
            throw new AuthServiceException("客户端[" + applicationClient.getClientName() + "]关联的Saml2配置数据丢失，请检查数据一致性");
        }
        if(StringUtils.hasText(saveDTO.clientName())){
            saml2RegisteredClient.setClientName(saveDTO.clientName());
        }
        if(StringUtils.hasText(saveDTO.entityId())){
            saml2RegisteredClient.setEntityId(saveDTO.entityId());
        }
        if(StringUtils.hasText(saveDTO.acsUrl())){
            saml2RegisteredClient.setAcsUrl(saveDTO.acsUrl());
        }
        if (Boolean.TRUE.equals(saveDTO.requireSignedAuthnRequest()) && !StringUtils.hasText(saveDTO.verificationCertificate())) {
            throw new AuthServiceException("AuthnRequest 必须签名时, 请求签名验证证书不能为空");
        }
        if(saveDTO.requireSignedAuthnRequest() != null){
            saml2RegisteredClient.setRequireSignedAuthnRequest(saveDTO.requireSignedAuthnRequest());
        }
        if (StringUtils.hasText(saveDTO.verificationCertificate())) {
            saml2RegisteredClient.setVerificationCertificate(saveDTO.verificationCertificate());
            saml2RegisteredClient.setRequireSignedAuthnRequest(true);
        }
        if(saveDTO.signResponse() != null){
            saml2RegisteredClient.setSignResponse(saveDTO.signResponse());
        }
        if(saveDTO.signAssertion() != null){
            saml2RegisteredClient.setSignAssertion(saveDTO.signAssertion());
        }
        if(saveDTO.attributeMappings() != null && !saveDTO.attributeMappings().isEmpty()){
            saml2RegisteredClient.setAttributeMappings(saveDTO.attributeMappings());
        }
        saml2RegisteredClientService.updateById(saml2RegisteredClient);

        ApplicationClientSaveDTO clientSaveDTO = ApplicationClientSaveDTO.of(applicationId, saveDTO, saml2RegisteredClient);
        applicationClientService.saveInfo(clientSaveDTO);

        log.info("Saml2 客户端配置更新成功, clientName: {}", applicationClient.getClientName());

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "修改Saml2客户端", AUDIT_LOG_TARGET));

    }

}
