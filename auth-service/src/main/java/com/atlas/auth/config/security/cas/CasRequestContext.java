package com.atlas.auth.config.security.cas;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CasRequestContext{

    private String service;

    private String serviceHash;

    private boolean renew;

    private boolean gateway;

    private String relayState;

}
