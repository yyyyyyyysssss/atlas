package com.atlas.auth.config.security.certificate;

import com.atlas.auth.config.security.certificate.model.CertificateProfile;
import com.atlas.auth.config.security.certificate.model.CertificateSubject;
import com.atlas.auth.config.security.certificate.model.IssuedCertificate;
import com.atlas.common.crypto.digest.DigestUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/13 11:39
 */
public class CertificateIssueService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CertificateKeyService keyService;
    private final CsrValidator csrValidator;
    private final CertificateConfig.CaMaterial caMaterial;

    public CertificateIssueService(CertificateKeyService keyService, CsrValidator csrValidator, CertificateConfig.CaMaterial caMaterial) {
        this.keyService = keyService;
        this.csrValidator = csrValidator;
        this.caMaterial = caMaterial;
    }

    /**
     * Atlas 生成密钥对并签发证书。
     */
    public IssuedCertificate issueGenerated(CertificateProfile profile, CertificateSubject subject) {
        validateProfile(profile);
        validateSubject(subject);

        KeyPair keyPair = keyService.generate(profile);

        return issueCertificate(
                keyPair.getPublic(),
                keyPair.getPrivate(),
                subject,
                profile
        );
    }

    /**
     * 客户端提交 CSR，Atlas 验证后签发证书。
     */
    public IssuedCertificate issueFromCsr(String csr, CertificateProfile profile, CertificateSubject subject) {
        validateProfile(profile);
        validateSubject(subject);

        PublicKey publicKey = csrValidator.validate(csr, profile);

        return issueCertificate(
                publicKey,
                null,
                subject,
                profile
        );
    }

    private IssuedCertificate issueCertificate(PublicKey publicKey, PrivateKey privateKey, CertificateSubject subject, CertificateProfile profile) {
        try {
            X509Certificate caCertificate = caMaterial.certificate();

            caCertificate.checkValidity();

            Instant now = Instant.now();
            Instant notBefore = now.minus(1, ChronoUnit.MINUTES);
            Instant requestedNotAfter = now.plus(profile.validityDays(), ChronoUnit.DAYS);

            Instant notAfter = requestedNotAfter
                    .isBefore(caCertificate.getNotAfter().toInstant())
                    ? requestedNotAfter
                    : caCertificate.getNotAfter().toInstant();

            if (!notAfter.isAfter(now)) {
                throw new IllegalStateException("Issued certificate would exceed CA validity");
            }

            BigInteger serialNumber = generateSerialNumber();

            X500Name issuer = new JcaX509CertificateHolder(caCertificate)
                    .getSubject();

            X500Name certificateSubject = buildSubject(subject);

            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    issuer,
                    serialNumber,
                    Date.from(notBefore),
                    Date.from(notAfter),
                    certificateSubject,
                    publicKey
            );

            JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();

            // CA:FALSE
            builder.addExtension(
                    Extension.basicConstraints,
                    true,
                    new BasicConstraints(false)
            );

            // TLS client certificate: digitalSignature
            builder.addExtension(
                    Extension.keyUsage,
                    true,
                    new KeyUsage(KeyUsage.digitalSignature)
            );

            // Extended Key Usage: clientAuth
            builder.addExtension(
                    Extension.extendedKeyUsage,
                    false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth)
            );

            // Subject Key Identifier
            builder.addExtension(
                    Extension.subjectKeyIdentifier,
                    false,
                    extensionUtils.createSubjectKeyIdentifier(publicKey)
            );

            // Authority Key Identifier
            builder.addExtension(
                    Extension.authorityKeyIdentifier,
                    false,
                    extensionUtils.createAuthorityKeyIdentifier(caCertificate)
            );

            String signatureAlgorithm = getSignatureAlgorithm(
                    caMaterial.privateKey().getAlgorithm()
            );

            ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
                    .setProvider("BC")
                    .build(caMaterial.privateKey());

            X509Certificate certificate = new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(builder.build(signer));

            // 验证签发结果
            certificate.verify(caCertificate.getPublicKey());

            certificate.checkValidity();

            if (certificate.getBasicConstraints() >= 0) {
                throw new IllegalStateException("Issued certificate must not be a CA certificate");
            }

            boolean[] keyUsage = certificate.getKeyUsage();

            if (keyUsage == null || keyUsage.length == 0 || !keyUsage[0]) {
                throw new IllegalStateException("Issued certificate does not permit digitalSignature");
            }

            List<X509Certificate> chain = Arrays.asList(caMaterial.chain());

            return new IssuedCertificate(
                    certificate,
                    privateKey,
                    chain,
                    serialNumber.toString(16).toUpperCase(),
                    DigestUtils.sha256(certificate.getEncoded()),
                    notBefore,
                    notAfter
            );

        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue X.509 certificate", e);
        }
    }

    private X500Name buildSubject(CertificateSubject subject) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

        builder.addRDN(
                BCStyle.CN,
                subject.commonName()
        );

        if (subject.organization() != null && !subject.organization().isBlank()) {
            builder.addRDN(
                    BCStyle.O,
                    subject.organization()
            );
        }

        if (subject.organizationalUnit() != null && !subject.organizationalUnit().isBlank()) {
            builder.addRDN(
                    BCStyle.OU,
                    subject.organizationalUnit()
            );
        }

        return builder.build();
    }

    private BigInteger generateSerialNumber() {
        // RFC 5280：正数，最多 20 个八位字节。
        // 使用 159 bit，最高位不置位，确保正数。
        return new BigInteger(159, SECURE_RANDOM).setBit(158);
    }

    private String getSignatureAlgorithm(String keyAlgorithm) {
        return switch (keyAlgorithm.toUpperCase()) {
            case "RSA" -> "SHA256WITHRSA";
            case "EC" -> "SHA256WITHECDSA";
            default -> throw new IllegalArgumentException("Unsupported CA key algorithm: " + keyAlgorithm);
        };
    }

    private void validateProfile(CertificateProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Certificate profile is required");
        }

        if (profile.validityDays() < 1 || profile.validityDays() > 825) {
            throw new IllegalArgumentException("Invalid certificate validity");
        }

        if ("RSA".equals(profile.keyAlgorithm())) {
            if (profile.keySize() != 2048 && profile.keySize() != 3072) {
                throw new IllegalArgumentException("Unsupported RSA key size");
            }
        } else if ("EC".equals(profile.keyAlgorithm())) {
            if (!"secp256r1".equals(profile.ecCurve()) && !"secp384r1".equals(profile.ecCurve())) {
                throw new IllegalArgumentException("Unsupported EC curve");
            }
        } else {
            throw new IllegalArgumentException("Unsupported key algorithm");
        }
    }

    private void validateSubject(CertificateSubject subject) {
        if (subject == null || subject.commonName() == null || subject.commonName().isBlank()) {
            throw new IllegalArgumentException("Certificate common name is required");
        }

        if (subject.commonName().length() > 64 || length(subject.organization()) > 64 || length(subject.organizationalUnit()) > 64) {
            throw new IllegalArgumentException("Certificate subject field is too long");
        }
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

}
