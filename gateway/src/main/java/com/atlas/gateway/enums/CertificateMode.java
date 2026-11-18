package com.atlas.gateway.enums;

public enum CertificateMode {

    /**
     * 从本地 HTTPS 请求属性获取
     */
    LOCAL,

    /**
     * 从反向代理请求头获取
     */
    FORWARDED

}
