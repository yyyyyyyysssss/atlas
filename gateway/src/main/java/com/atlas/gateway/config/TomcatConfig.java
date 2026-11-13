package com.atlas.gateway.config;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 *@Description 本地开发环境专用 HTTPS Connector。
 *@Author ys
 *@Date 2026/11/13 9:44
 */
@Configuration
@Profile("dev")
public class TomcatConfig {

    /*
     * 本地开发环境专用 HTTPS Connector。
     * HTTPS 测试端口：8443
     * 用于本地 HTTPS 调试及浏览器客户端证书认证测试。
     * 生产环境不加载此配置。
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> localHttpsConnectorCustomizer(
            Environment environment,
            ResourceLoader resourceLoader) {
        String httpsPort = environment.getProperty("security.local-https.port");
        String keyStore = environment.getProperty("security.local-https.key-store");
        String keyStorePassword = environment.getProperty("security.local-https.key-store-password");
        String keyAlias = environment.getProperty("security.local-https.key-alias");
        // 配置不完整时，不注册 HTTPS Connector。
        if (!StringUtils.hasText(httpsPort) || !StringUtils.hasText(keyStore) || !StringUtils.hasText(keyStorePassword) || !StringUtils.hasText(keyAlias)) {
            return factory -> {
            };
        }
        return factory -> {
            try {
                Resource resource = resourceLoader.getResource(keyStore);
                File tempFile = File.createTempFile("atlas-local-", ".p12");
                tempFile.deleteOnExit();

                try (InputStream inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
                connector.setPort(Integer.parseInt(httpsPort));
                connector.setScheme("https");
                connector.setSecure(true);

                connector.setProperty("SSLEnabled", "true");

                SSLHostConfig sslHostConfig = new SSLHostConfig();
                SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
                certificate.setCertificateKeystoreFile(tempFile.getAbsolutePath());
                certificate.setCertificateKeystorePassword(keyStorePassword);
                certificate.setCertificateKeystoreType("PKCS12");
                certificate.setCertificateKeyAlias(keyAlias);
                sslHostConfig.addCertificate(certificate);
                connector.addSslHostConfig(sslHostConfig);

                factory.addAdditionalTomcatConnectors(connector);

            } catch (IOException e) {
                throw new IllegalStateException("Failed to load local HTTPS certificate", e);
            }
        };
    }

}
