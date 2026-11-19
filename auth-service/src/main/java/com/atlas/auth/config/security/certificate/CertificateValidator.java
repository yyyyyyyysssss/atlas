package com.atlas.auth.config.security.certificate;

import com.atlas.common.crypto.digest.DigestUtils;

import java.security.GeneralSecurityException;
import java.security.cert.*;
import java.util.*;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/19 10:23
 */
public class CertificateValidator {

    private static final String CLIENT_AUTH_OID = "1.3.6.1.5.5.7.3.2";

    // 最大证书路径长度
    private static final int MAX_PATH_LENGTH = 5;

    private final X509Certificate caCertificate;

    private final X509Certificate[] chain;

    public CertificateValidator(CertificateConfig.CaMaterial caMaterial) {
        this.caCertificate = caMaterial.certificate();
        this.chain = caMaterial.chain();
    }

    /**
     * 验证客户端证书及其证书链。
     *
     * @param certificate      客户端终端证书
     * @param certificateChain 客户端提交的证书链，不包含终端证书
     */
    public void validate(X509Certificate certificate, Collection<X509Certificate> certificateChain) throws GeneralSecurityException {
        if (certificate == null) {
            throw new GeneralSecurityException("Client certificate is missing");
        }

        validateClientCertificate(certificate);

        List<X509Certificate> chain = certificateChain == null ? List.of() : new ArrayList<>(certificateChain);

        validateCertificateChainInput(certificate, chain);

        validateCertificatePath(certificate, chain);

    }

    // 校验客户端终端证书自身属性。
    private void validateClientCertificate(X509Certificate certificate) throws GeneralSecurityException {
        //  有效期
        certificate.checkValidity();

        // 终端证书不能是 CA
        if (certificate.getBasicConstraints() >= 0) {
            throw new GeneralSecurityException("Client certificate must not be a CA certificate");
        }

        //  Key Usage：要求 digitalSignature
        boolean[] keyUsage = certificate.getKeyUsage();

        if (keyUsage == null || keyUsage.length == 0 || !keyUsage[0]) {
            throw new GeneralSecurityException("Client certificate does not permit digitalSignature");
        }

        // Extended Key Usage：要求 clientAuth
        List<String> extendedKeyUsage = certificate.getExtendedKeyUsage();

        if (extendedKeyUsage == null || !extendedKeyUsage.contains(CLIENT_AUTH_OID)) {
            throw new GeneralSecurityException("Client certificate does not permit client authentication");
        }
    }

    // 校验客户端提交的证书链格式及基本约束。
    private void validateCertificateChainInput(X509Certificate certificate, List<X509Certificate> certificateChain) throws GeneralSecurityException {
        if (certificateChain.size() > MAX_PATH_LENGTH) {
            throw new GeneralSecurityException("Certificate chain exceeds maximum length");
        }

        Set<String> fingerprints = new HashSet<>();
        fingerprints.add(DigestUtils.sha256(certificate.getEncoded()));

        for (X509Certificate chainCertificate : certificateChain) {
            if (chainCertificate == null) {
                throw new GeneralSecurityException("Certificate chain contains null certificate");
            }

            String fingerprint = DigestUtils.sha256(chainCertificate.getEncoded());
            if (!fingerprints.add(fingerprint)) {
                throw new GeneralSecurityException("Duplicate certificate in certificate chain");
            }
            // 证书链中的证书必须是 CA
            if (chainCertificate.getBasicConstraints() < 0) {
                throw new GeneralSecurityException("Certificate chain contains a non-CA certificate");
            }

            boolean[] keyUsage = chainCertificate.getKeyUsage();

            if (keyUsage != null && (keyUsage.length <= 5 || !keyUsage[5])) {
                throw new GeneralSecurityException("Intermediate CA does not permit keyCertSign");
            }

            chainCertificate.checkValidity();
        }
    }

    // 使用 Atlas CA 作为信任锚验证证书路径。
    private void validateCertificatePath(X509Certificate certificate, List<X509Certificate> certificateChain) throws GeneralSecurityException {

        caCertificate.checkValidity();

        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(certificate);

        TrustAnchor trustAnchor = new TrustAnchor(caCertificate, null);

        PKIXBuilderParameters parameters = new PKIXBuilderParameters(Set.of(trustAnchor), selector);

        parameters.setMaxPathLength(MAX_PATH_LENGTH);

        // 证书链由请求头传递
        List<X509Certificate> certificates = new ArrayList<>(certificateChain);

        // 加入本地 CA 证书链，作为路径构建候选证书
        for (X509Certificate caChainCertificate : chain) {
            if (!caChainCertificate.equals(caCertificate)) {
                certificates.add(caChainCertificate);
            }
        }

        if (!certificates.isEmpty()) {
            CertStore certStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certificates));
            parameters.addCertStore(certStore);
        }

        /*
         * 吊销状态由 Atlas 用户证书表负责检查。
         * 这里不启用 PKIX 的 CRL / OCSP 检查。
         */
        parameters.setRevocationEnabled(false);

        CertPathBuilder.getInstance("PKIX").build(parameters);
    }

}
