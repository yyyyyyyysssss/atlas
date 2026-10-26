package com.atlas.auth.domain.dto;

import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.atlas.auth.enums.ClientProtocol;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

public record ApplicationClientSaveDTO(
        Long id,

        Long applicationId,

        String registeredClientId,

        String clientId,

        String clientName,

        ClientProtocol clientProtocol,

        String description
) {

    public static ApplicationClientSaveDTO of(Long applicationId, OAuth2ClientSaveDTO saveDTO, RegisteredClient registeredClient){

        return new ApplicationClientSaveDTO(
                saveDTO.id(),
                applicationId,
                registeredClient.getId(),
                registeredClient.getClientId(),
                saveDTO.clientName(),
                ClientProtocol.OAUTH2,
                saveDTO.description()
        );
    }

    public static ApplicationClientSaveDTO of(Long applicationId, Saml2ClientSaveDTO saveDTO, Saml2RegisteredClient registeredClient){

        return new ApplicationClientSaveDTO(
                saveDTO.id(),
                applicationId,
                registeredClient.getId(),
                registeredClient.getEntityId(),
                saveDTO.clientName(),
                ClientProtocol.SAML2,
                saveDTO.description()
        );
    }

}