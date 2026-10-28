package com.atlas.auth.domain.vo;

import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.domain.entity.Saml2RegisteredClient;

import java.time.LocalDateTime;
import java.util.Map;

public record Saml2ClientVO(

        Long id,

        String clientName,

        String entityId,

        String acsUrl,

        String verificationCertificate,

        Boolean requireSignedAuthnRequest,

        Boolean signAssertion,

        Boolean signResponse,

        Map<String, String> attributeMappings,

        String description,

        LocalDateTime createTime,

        LocalDateTime lastUsedTime

) {

    public static Saml2ClientVO of(ApplicationClient applicationClient, Saml2RegisteredClient registeredClient) {

        return new Saml2ClientVO(
                applicationClient.getId(),
                applicationClient.getClientName(),
                registeredClient.getEntityId(),
                registeredClient.getAcsUrl(),
                registeredClient.getVerificationCertificate(),
                registeredClient.getRequireSignedAuthnRequest(),
                registeredClient.getSignAssertion(),
                registeredClient.getSignResponse(),
                registeredClient.getAttributeMappings(),
                applicationClient.getDescription(),
                applicationClient.getCreateTime(),
                applicationClient.getLastUsedTime()
        );
    }

}
