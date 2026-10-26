package com.atlas.auth.domain.vo;

import com.atlas.auth.domain.entity.ApplicationClient;

import java.time.LocalDateTime;

public record ApplicationClientVO(
        Long id,
        String clientId,
        String clientName,
        String description,
        LocalDateTime createTime
) {

    public static ApplicationClientVO of(ApplicationClient applicationClient){
        return new ApplicationClientVO(
                applicationClient.getId(),
                applicationClient.getClientId(),
                applicationClient.getClientName(),
                applicationClient.getDescription(),
                applicationClient.getCreateTime()
        );
    }

}
