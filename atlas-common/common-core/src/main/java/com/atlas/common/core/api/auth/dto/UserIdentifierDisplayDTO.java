package com.atlas.common.core.api.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserIdentifierDisplayDTO {

    private Long userId;

    private String username;

    private String email;

    private String phone;

    private String initPassword;
}
