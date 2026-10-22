package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.Saml2ClientApplicationSaveDTO;
import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.atlas.auth.domain.vo.ProjectVO;
import com.atlas.auth.domain.vo.Saml2ClientApplicationCreateVO;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.crypto.random.SecureRandomUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
public class Saml2ClientApplicationFacadeService extends AbstractClientApplicationFacadeService{

    private final Saml2RegisteredClientService saml2RegisteredClientService;


    public Saml2ClientApplicationCreateVO save(String projectCode, Saml2ClientApplicationSaveDTO saveDTO) {
        Saml2ClientApplicationCreateVO vo = null;
        if (saveDTO.id() == null) {
            vo = createApplication(projectCode, saveDTO);
        } else {
            updateApplication(projectCode, saveDTO);
        }
        return vo;
    }

    private Saml2ClientApplicationCreateVO createApplication(String projectCode, Saml2ClientApplicationSaveDTO saveDTO){
        log.info("正在创建 Saml2 应用，名称: {}", saveDTO.applicationName());
        if(Boolean.TRUE.equals(saveDTO.requireSignedAuthnRequest()) && !StringUtils.hasText(saveDTO.verificationCertificate())){
            throw new AuthServiceException("AuthnRequest 必须签名时, 请求签名验证证书不能为空");
        }
        boolean requireSignedAuthnRequest = saveDTO.requireSignedAuthnRequest() != null ? saveDTO.requireSignedAuthnRequest() : false;
        if(StringUtils.hasText(saveDTO.verificationCertificate())){
            requireSignedAuthnRequest = true;
        }
        boolean signResponse = saveDTO.signResponse() != null ? saveDTO.signResponse() : true;
        boolean signAssertion = saveDTO.signAssertion() != null ? saveDTO.signAssertion() : true;
        ProjectVO projectVO = checkAndEnsureProjectActive(projectCode);
        String registeredClientId = UUID.randomUUID().toString().replace("-", "");
        String clientId = SecureRandomUtils.generate(20);
        Saml2RegisteredClient saml2RegisteredClient = Saml2RegisteredClient.builder()
                .id(registeredClientId)
                .clientId(clientId)
                .clientName(saveDTO.applicationName())
                .entityId(saveDTO.entityId())
                .acsUrl(saveDTO.acsUrl())
                .requireSignedAuthnRequest(requireSignedAuthnRequest)
                .verificationCertificate(saveDTO.verificationCertificate())
                .signResponse(signResponse)
                .signAssertion(signAssertion)
                .attributeMappings(saveDTO.attributeMappings())
                .build();

        return null;
    }

    private void updateApplication(String projectCode, Saml2ClientApplicationSaveDTO saveDTO) {

    }

}
