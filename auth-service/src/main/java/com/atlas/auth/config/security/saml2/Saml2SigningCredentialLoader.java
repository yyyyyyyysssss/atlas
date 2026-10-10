package com.atlas.auth.config.security.saml2;

import lombok.extern.slf4j.Slf4j;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/10 9:58
 */
@Slf4j
@Component
public class Saml2SigningCredentialLoader {

    private final ResourceLoader resourceLoader;

    public Saml2SigningCredentialLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Saml2SigningCredential load(String keyStoreLocation, String keyStorePassword, String keyAlias) {
        requireText(keyStoreLocation, "SAML 签名密钥库路径未配置");
        requireText(keyStorePassword, "SAML 签名密钥库密码未配置");
        requireText(keyAlias, "SAML 签名密钥别名未配置");
        try {
            Resource resource = resourceLoader.getResource(keyStoreLocation);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            char[] password = keyStorePassword.toCharArray();

            try (InputStream input = resource.getInputStream()) {
                keyStore.load(input, password);
            }
            if (!keyStore.containsAlias(keyAlias)) {
                throw new IllegalStateException("SAML IdP 签名密钥库中不存在别名: " + keyAlias);
            }

            Key key = keyStore.getKey(keyAlias, password);

            if (!(key instanceof PrivateKey privateKey)) {
                throw new IllegalStateException("SAML IdP 指定条目不包含私钥: " + keyAlias);
            }

            Certificate certificate = keyStore.getCertificate(keyAlias);

            if (!(certificate instanceof X509Certificate x509Certificate)) {
                throw new IllegalStateException("SAML IdP 指定条目不包含 X.509 证书: " + keyAlias);
            }

            x509Certificate.checkValidity();

            log.info("[SAML2] 签名凭证加载成功，alias={}, algorithm={}", keyAlias, privateKey.getAlgorithm());

            return new Saml2SigningCredential(privateKey, x509Certificate);
        } catch (Exception e) {
            throw new IllegalStateException("加载 SAML 签名凭证失败，alias=" + keyAlias, e);
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }

    public record Saml2SigningCredential(PrivateKey privateKey, X509Certificate certificate) {

        public Credential toOpenSamlCredential() {
            return new BasicX509Credential(certificate, privateKey);
        }

        public Saml2X509Credential toSpringCredential() {
            return Saml2X509Credential.signing(privateKey, certificate);
        }
    }

}
