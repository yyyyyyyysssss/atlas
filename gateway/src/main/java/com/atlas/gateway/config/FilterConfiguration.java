package com.atlas.gateway.config;

import com.atlas.gateway.filter.ClientCertificateFilterSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/18 16:50
 */

@Configuration
public class FilterConfiguration {

    @Bean
    public ClientCertificateFilterSupplier clientCertificateFilterSupplier() {
        return new ClientCertificateFilterSupplier();
    }

}
