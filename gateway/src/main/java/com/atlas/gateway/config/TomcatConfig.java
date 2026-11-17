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
        // HTTPS 服务端证书配置
        String httpsPort = environment.getProperty("security.local-https.port");
        String keyStore = environment.getProperty("security.local-https.key-store");
        String keyStorePassword = environment.getProperty("security.local-https.key-store-password");
        String keyAlias = environment.getProperty("security.local-https.key-alias");


        // Atlas CA 客户端证书信任库配置
        String trustStore = environment.getProperty("security.local-https.trust-store");
        String trustStorePassword = environment.getProperty("security.local-https.trust-store-password");
        String trustStoreType = environment.getProperty("security.local-https.trust-store-type", "PKCS12");


        // 配置不完整时，不注册 HTTPS Connector。
        if (!StringUtils.hasText(httpsPort)
                || !StringUtils.hasText(keyStore)
                || !StringUtils.hasText(keyStorePassword)
                || !StringUtils.hasText(keyAlias)
                || !StringUtils.hasText(trustStore)
                || !StringUtils.hasText(trustStorePassword)) {
            return factory -> {
            };
        }
        return factory -> {
            try {
                // 加载服务端 HTTPS 证书
                File keyStoreFile = copyToTempFile(
                        resourceLoader,
                        keyStore,
                        "atlas-local-key-",
                        ".p12"
                );

                // 加载 Atlas CA 客户端证书信任库
                File trustStoreFile = copyToTempFile(
                        resourceLoader,
                        trustStore,
                        "atlas-local-trust-",
                        ".p12"
                );


                // 创建 HTTPS Connector
                Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
                connector.setPort(Integer.parseInt(httpsPort));
                connector.setScheme("https");
                connector.setSecure(true);

                connector.setProperty("SSLEnabled", "true");

                // SSL 配置
                SSLHostConfig sslHostConfig = new SSLHostConfig();

                // 强制要求客户端提供有效证书
                sslHostConfig.setCertificateVerification("required");
                // 配置 Atlas CA 客户端证书信任库
                sslHostConfig.setTruststoreFile(trustStoreFile.getAbsolutePath());
                sslHostConfig.setTruststorePassword(trustStorePassword);
                sslHostConfig.setTruststoreType(trustStoreType);


                // 配置服务端证书
                SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
                certificate.setCertificateKeystoreFile(keyStoreFile.getAbsolutePath());
                certificate.setCertificateKeystorePassword(keyStorePassword);
                certificate.setCertificateKeystoreType("PKCS12");
                certificate.setCertificateKeyAlias(keyAlias);

                sslHostConfig.addCertificate(certificate);
                connector.addSslHostConfig(sslHostConfig);

                // 注册额外 HTTPS Connector
                factory.addAdditionalTomcatConnectors(connector);

            } catch (IOException e) {
                throw new IllegalStateException("Failed to load local HTTPS certificate", e);
            }
        };
    }

    /**
     * 将证书资源复制到临时文件。
     * 支持 classpath: 和 file: 等 Spring Resource 路径。
     */
    private File copyToTempFile(ResourceLoader resourceLoader, String location, String prefix, String suffix) throws IOException {

        Resource resource = resourceLoader.getResource(location);

        if (!resource.exists()) {
            throw new IOException("Certificate resource not found: " + location);
        }

        File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();

        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }

}
