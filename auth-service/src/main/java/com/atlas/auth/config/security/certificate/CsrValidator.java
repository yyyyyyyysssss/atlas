package com.atlas.auth.config.security.certificate;

import com.atlas.auth.config.security.certificate.model.CertificateProfile;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;

public class CsrValidator {

    private static final int MAX_CSR_SIZE = 16 * 1024;

    private static final Set<ASN1ObjectIdentifier> ALLOWED_SIGNATURES = Set.of(
            PKCSObjectIdentifiers.sha256WithRSAEncryption,
            PKCSObjectIdentifiers.sha384WithRSAEncryption,
            X9ObjectIdentifiers.ecdsa_with_SHA256,
            X9ObjectIdentifiers.ecdsa_with_SHA384
    );

    private static final ASN1ObjectIdentifier SECP256R1 =
            new ASN1ObjectIdentifier("1.2.840.10045.3.1.7");

    private static final ASN1ObjectIdentifier SECP384R1 =
            new ASN1ObjectIdentifier("1.3.132.0.34");

    public PublicKey validate(String csr, CertificateProfile profile) {
        try {
            if (csr == null || csr.isBlank()) {
                throw new IllegalArgumentException("CSR must not be empty");
            }

            if (csr.getBytes(StandardCharsets.UTF_8).length > MAX_CSR_SIZE) {
                throw new IllegalArgumentException("CSR exceeds maximum size");
            }

            PKCS10CertificationRequest request = parse(csr);

            // 校验 CSR 签名算法
            ASN1ObjectIdentifier signatureAlgorithm =
                    request.getSignatureAlgorithm().getAlgorithm();

            if (!ALLOWED_SIGNATURES.contains(signatureAlgorithm)) {
                throw new IllegalArgumentException(
                        "Unsupported CSR signature algorithm"
                );
            }

            // 提取公钥
            PublicKey publicKey = new JcaPKCS10CertificationRequest(request)
                    .setProvider("BC")
                    .getPublicKey();

            // 验证 CSR 签名
            boolean valid = request.isSignatureValid(
                    new JcaContentVerifierProviderBuilder()
                            .setProvider("BC")
                            .build(publicKey)
            );

            if (!valid) {
                throw new IllegalArgumentException(
                        "CSR signature verification failed"
                );
            }

            // 校验公钥算法、长度及曲线
            validatePublicKey(request, publicKey, profile);

            return publicKey;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid certificate signing request", e
            );
        }
    }

    /**
     * 解析单个 PKCS#10 PEM 对象。
     */
    private PKCS10CertificationRequest parse(String csr) throws Exception {

        try (PemReader reader = new PemReader(new StringReader(csr))) {

            PemObject pemObject = reader.readPemObject();

            if (pemObject == null) {
                throw new IllegalArgumentException("Empty CSR");
            }

            String type = pemObject.getType();

            if (!"CERTIFICATE REQUEST".equals(type)
                    && !"NEW CERTIFICATE REQUEST".equals(type)) {
                throw new IllegalArgumentException(
                        "Invalid CSR PEM type"
                );
            }

            // 不允许包含多个 PEM 对象
            if (reader.readPemObject() != null) {
                throw new IllegalArgumentException(
                        "Multiple PEM objects are not allowed"
                );
            }

            return new PKCS10CertificationRequest(
                    pemObject.getContent()
            );
        }
    }

    /**
     * 校验 CSR 公钥。
     */
    private void validatePublicKey(
            PKCS10CertificationRequest request,
            PublicKey publicKey,
            CertificateProfile profile
    ) {
        switch (profile.keyAlgorithm()) {

            case "RSA" -> {
                if (!(publicKey instanceof RSAPublicKey rsaPublicKey)) {
                    throw new IllegalArgumentException(
                            "CSR public key must be RSA"
                    );
                }

                int keySize = rsaPublicKey.getModulus().bitLength();

                if (keySize < 2048 || keySize != profile.keySize()) {
                    throw new IllegalArgumentException(
                            "CSR RSA key size does not match profile"
                    );
                }
            }

            case "EC" -> {
                if (!(publicKey instanceof ECPublicKey)) {
                    throw new IllegalArgumentException(
                            "CSR public key must be EC"
                    );
                }

                ASN1ObjectIdentifier algorithm =
                        request.getSubjectPublicKeyInfo()
                                .getAlgorithm()
                                .getAlgorithm();

                if (!X9ObjectIdentifiers.id_ecPublicKey.equals(algorithm)) {
                    throw new IllegalArgumentException(
                            "Invalid EC public key algorithm"
                    );
                }

                ASN1ObjectIdentifier curveOid =
                        ASN1ObjectIdentifier.getInstance(
                                request.getSubjectPublicKeyInfo()
                                        .getAlgorithm()
                                        .getParameters()
                        );

                ASN1ObjectIdentifier expectedOid = switch (
                        profile.ecCurve()
                        ) {
                    case "secp256r1" -> SECP256R1;
                    case "secp384r1" -> SECP384R1;
                    default -> throw new IllegalArgumentException(
                            "Unsupported EC curve"
                    );
                };

                if (!expectedOid.equals(curveOid)) {
                    throw new IllegalArgumentException(
                            "CSR EC curve does not match profile"
                    );
                }
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported key algorithm"
            );
        }
    }
}
