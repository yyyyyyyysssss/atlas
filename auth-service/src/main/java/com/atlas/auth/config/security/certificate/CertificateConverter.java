package com.atlas.auth.config.security.certificate;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
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

        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("PKCS12 password must contain at least 8 characters");
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

    /**
     * DER 字节数组转换为 X.509 证书。
     */
    public X509Certificate fromDer(byte[] der) {
        if (der == null || der.length == 0) {
            throw new IllegalArgumentException("Certificate DER is required");
        }
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der));
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Failed to convert DER to X.509 certificate", e);
        }
    }

    /**
     * DER 字节数组列表转换为 X.509 证书链。
     */
    public List<X509Certificate> chainFromDer(List<byte[]> derList) {
        if (derList == null || derList.isEmpty()) {
            return List.of();
        }
        List<X509Certificate> certificateChain = new ArrayList<>(derList.size());

        for (byte[] der : derList) {
            certificateChain.add(fromDer(der));
        }

        return List.copyOf(certificateChain);
    }

    /**
     * RFC 9440 Client-Cert 转换为 X.509 证书。
     * 格式：:Base64(DER):
     */
    public X509Certificate fromClientCert(String clientCert) {
        byte[] der = decodeByteSequence(clientCert);
        return fromDer(der);
    }

    /**
     * RFC 9440 Client-Cert-Chain 转换为 X.509 证书链。
     * 格式：:Base64(DER):, :Base64(DER):
     */
    public List<X509Certificate> chainFromClientCert(String clientCertChain) {
        if (clientCertChain == null || clientCertChain.isBlank()) {
            return List.of();
        }

        List<byte[]> derList = new ArrayList<>();

        String[] values = clientCertChain.split(",");

        for (String value : values) {
            derList.add(decodeByteSequence(value.trim()));
        }

        return chainFromDer(derList);
    }

    /**
     * 解析 RFC 9440 Byte Sequence。
     */
    private byte[] decodeByteSequence(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Certificate byte sequence is required");
        }

        String content = value.trim();

        if (!content.startsWith(":") || !content.endsWith(":")) {
            throw new IllegalArgumentException("Invalid RFC 9440 byte sequence");
        }

        String base64 = content.substring(1, content.length() - 1);

        if (base64.isEmpty()) {
            throw new IllegalArgumentException("Certificate byte sequence is empty");
        }

        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid certificate Base64 encoding", e);
        }
    }

}
