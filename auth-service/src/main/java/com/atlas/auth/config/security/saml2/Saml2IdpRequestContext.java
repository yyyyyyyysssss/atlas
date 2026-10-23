package com.atlas.auth.config.security.saml2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 9:33
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Saml2IdpRequestContext {

    /**
     * AuthnRequest ID
     */
    private String requestId;

    /**
     * SP EntityID
     */
    private String spEntityId;

    /**
     * 注册的SP客户端ID
     */
    private String registeredClientId;

    /**
     * ACS地址
     */
    private String acsUrl;


    /**
     * RelayState
     */
    private String relayState;


    /**
     * 响应绑定
     */
    private String responseBinding;


    /**
     * NameID格式
     */
    private String nameIdFormat;

    /**
     * 是否强制重新认证 必须重新认证，即使已有登录态
     */
    private boolean forceAuthn;

    /**
     * 是否禁止交互认证 只能使用已有登录态
     */
    private boolean passive;

    /**
     * 创建时间
     */
    private Instant createdAt;

}
