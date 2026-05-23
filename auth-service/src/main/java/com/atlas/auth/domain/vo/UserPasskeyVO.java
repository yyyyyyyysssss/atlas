package com.atlas.auth.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserPasskeyVO {

    private String credentialId;

    private Long userId;

    private String label;

    private LocalDateTime createTime;


}
