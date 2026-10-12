package com.atlas.auth.config.security.saml2;

import lombok.Builder;
import lombok.Data;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 16:44
 */
@Data
@Builder
public class Saml2IdpResponse {

    /**
     * REDIRECT / POST
     */
    private String binding;


    /**
     * REDIRECT 使用
     */
    private String redirectUrl;


    /**
     * POST 使用
     */
    private String acsUrl;

    private String samlResponse;

    private String relayState;

}
