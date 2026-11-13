package com.atlas.auth.config.security.certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/13 11:08
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CertificateProperties.class)
@ConditionalOnProperty(
        prefix = "security.certificate",
        name = "enabled",
        havingValue = "true"
)
public class CertificateConfig {


    @Bean
    public CaMaterial caMaterial(CertificateProperties properties, ResourceLoader resourceLoader) throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        char[] password = properties.keyStorePassword().toCharArray();
        try {
            Resource resource = resourceLoader.getResource(properties.keyStore());

            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            try (InputStream inputStream = resource.getInputStream()) {
                keyStore.load(inputStream, password);
            }

            String alias = properties.keyAlias();

            if (!keyStore.containsAlias(alias)) {
                throw new IllegalStateException("CA key alias does not exist: " + alias);
            }

            if (!keyStore.isKeyEntry(alias)) {
                throw new IllegalStateException("CA alias is not a private key entry: " + alias);
            }

            Key key = keyStore.getKey(alias, password);

            if (!(key instanceof PrivateKey privateKey)) {
                throw new IllegalStateException("CA private key is unavailable");
            }

            if (!(keyStore.getCertificate(alias) instanceof X509Certificate caCertificate)) {
                throw new IllegalStateException("CA certificate is unavailable");
            }

            X509Certificate[] chain = Arrays.stream(keyStore.getCertificateChain(alias))
                    .map(certificate -> (X509Certificate) certificate)
                    .toArray(X509Certificate[]::new);

            validateCa(caCertificate, privateKey);

            return new CaMaterial(privateKey, caCertificate, chain);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Bean
    public CertificateKeyService certificateKeyService() {

        return new CertificateKeyService();
    }

    @Bean
    public CsrValidator csrValidator() {

        return new CsrValidator();
    }

    @Bean
    public CertificateIssueService certificateIssueService(CertificateKeyService keyService,
                                                           CsrValidator csrValidator,
                                                           CaMaterial caMaterial) {

        return new CertificateIssueService(keyService, csrValidator, caMaterial);
    }

    @Bean
    public CertificateConverter certificateConverter() {

        return new CertificateConverter();
    }


    private void validateCa(X509Certificate certificate, PrivateKey privateKey) throws Exception {

        certificate.checkValidity();

        if (!privateKey.getAlgorithm().equalsIgnoreCase(
                certificate.getPublicKey().getAlgorithm())) {
            throw new IllegalStateException(
                    "CA private key algorithm does not match CA certificate"
            );
        }

        if (certificate.getBasicConstraints() < 0) {
            throw new IllegalStateException(
                    "Configured certificate is not a CA certificate"
            );
        }

        boolean[] keyUsage = certificate.getKeyUsage();

        if (keyUsage == null
                || keyUsage.length <= 5
                || !keyUsage[5]) {
            throw new IllegalStateException(
                    "CA certificate does not permit keyCertSign"
            );
        }

        String signatureAlgorithm = switch (
                privateKey.getAlgorithm().toUpperCase()
                ) {
            case "RSA" -> "SHA256WITHRSA";
            case "EC" -> "SHA256WITHECDSA";
            default -> throw new IllegalStateException(
                    "Unsupported CA private key algorithm"
            );
        };

        byte[] challenge = new byte[32];
        new java.security.SecureRandom().nextBytes(challenge);

        java.security.Signature signature = java.security.Signature.getInstance(signatureAlgorithm);

        signature.initSign(privateKey);
        signature.update(challenge);
        byte[] signed = signature.sign();

        signature.initVerify(certificate.getPublicKey());
        signature.update(challenge);

        if (!signature.verify(signed)) {
            throw new IllegalStateException(
                    "CA private key does not match CA certificate"
            );
        }
    }

    public record CaMaterial(PrivateKey privateKey, X509Certificate certificate, X509Certificate[] chain) {

        public CaMaterial {
            chain = chain.clone();
        }

        @Override
        public X509Certificate[] chain() {
            return chain.clone();
        }
    }

}
