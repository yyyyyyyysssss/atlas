package com.atlas.auth.domain.vo;

import com.atlas.security.enums.ClientType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenValidVO {

    private Boolean active;

    private String subject;

    private ClientType clientType;

    private Long expiration;
    
}
