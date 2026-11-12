package com.atlas.auth.domain.vo;

import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.domain.entity.CasRegisteredClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record CasClientVO(

        Long id,

        String clientName,

        String service,

        Boolean proxyEnabled,

        List<String> proxyCallbackUrls,

        Map<String, String> attributeMappings,

        String description,

        LocalDateTime createTime,

        LocalDateTime lastUsedTime

) {

    public static CasClientVO of(ApplicationClient applicationClient, CasRegisteredClient registeredClient) {

        return new CasClientVO(
                applicationClient.getId(),
                applicationClient.getClientName(),
                registeredClient.getService(),
                registeredClient.getProxyEnabled(),
                registeredClient.getProxyCallbackUrls(),
                registeredClient.getAttributeMappings(),
                applicationClient.getDescription(),
                applicationClient.getCreateTime(),
                applicationClient.getLastUsedTime()
        );
    }

}
