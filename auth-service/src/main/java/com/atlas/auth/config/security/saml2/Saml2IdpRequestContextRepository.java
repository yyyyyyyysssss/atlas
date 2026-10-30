package com.atlas.auth.config.security.saml2;

public interface Saml2IdpRequestContextRepository {

    /**
     * 保存待恢复请求
     */
    void save(String issuer, Saml2IdpRequestContext context);


    /**
     * 获取请求上下文
     */
    Saml2IdpRequestContext consume(String issuer, String requestId);



    /**
     * 标记请求已经处理
     */
    boolean markAsSeen(String issuer, String requestId);

}
