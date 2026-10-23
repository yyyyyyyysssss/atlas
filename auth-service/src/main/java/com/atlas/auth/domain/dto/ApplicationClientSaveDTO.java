package com.atlas.auth.domain.dto;

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

    public static ApplicationClientSaveDTO of(Long applicationId, OAuth2ClientApplicationSaveDTO saveDTO, RegisteredClient registeredClient){

        return new ApplicationClientSaveDTO(
                saveDTO.id(),
                applicationId,
                registeredClient.getId(),
                registeredClient.getClientId(),
                registeredClient.getClientName(),
                ClientProtocol.OAUTH2,
                saveDTO.description()
        );
    }

}