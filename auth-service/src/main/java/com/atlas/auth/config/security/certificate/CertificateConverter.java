package com.atlas.auth.config.security.certificate;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/13 11:42
 */
public class CertificateConverter {

    /**
     * X.509 证书转换为 PEM。
     */
    public String toPem(X509Certificate certificate) {
        try {
            StringWriter writer = new StringWriter();

            try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
                pemWriter.writeObject(certificate);
            }

            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to convert certificate to PEM", e
            );
        }
    }

    /**
     * X.509 证书链转换为 PEM。
     * 顺序：签发 CA -> 上级 CA -> Root CA。
     */
    public String chainToPem(List<X509Certificate> certificateChain) {
        try {
            StringWriter writer = new StringWriter();

            try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
                for (X509Certificate certificate : certificateChain) {
                    pemWriter.writeObject(certificate);
                }
            }

            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert certificate chain to PEM", e);
        }
    }

    /**
     * 生成 PKCS#12。
     * certificateChain 不包含客户端叶子证书，
     * 只包含签发 CA 和上级 CA。
     */
    public byte[] toPkcs12(String alias, PrivateKey privateKey, X509Certificate certificate, List<X509Certificate> certificateChain, char[] password) {
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key is required");
        }

        if (password == null || password.length < 12) {
            throw new IllegalArgumentException("PKCS12 password must contain at least 12 characters");
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, password);

            List<Certificate> chain = new ArrayList<>();
            chain.add(certificate);
            chain.addAll(certificateChain);

            keyStore.setKeyEntry(
                    alias,
                    privateKey,
                    password,
                    chain.toArray(Certificate[]::new)
            );

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            keyStore.store(output, password);

            return output.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PKCS12", e);
        }
    }

}
