package com.atlas.gateway.filter;

import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/18 16:46
 */
public class ClientCertificateFilterSupplier extends SimpleFilterSupplier {

    public ClientCertificateFilterSupplier() {
        super(ClientCertificateFilterFunction.class);
    }
}
