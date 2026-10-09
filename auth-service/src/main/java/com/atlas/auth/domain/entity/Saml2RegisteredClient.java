package com.atlas.auth.domain.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/9 16:03
 */
@Getter
@Setter
public class Saml2RegisteredClient {

    private String acsUrl;

    private String entityId;

    private Boolean signAssertion;

    private Boolean signResponse;

}
