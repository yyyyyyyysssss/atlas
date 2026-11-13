package com.atlas.auth.config.security.certificate;

import com.atlas.auth.config.security.certificate.model.CertificateProfile;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Set;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/13 11:19
 */
public class CertificateKeyService {

    private static final Set<Integer> RSA_KEY_SIZES = Set.of(2048, 3072);

    private static final Set<String> EC_CURVES = Set.of("secp256r1", "secp384r1");

    private final SecureRandom secureRandom = new SecureRandom();

    public KeyPair generate(CertificateProfile profile) {
        try {
            return switch (profile.keyAlgorithm()) {
                case "RSA" -> generateRsa(profile);
                case "EC" -> generateEc(profile);
                default -> throw new IllegalArgumentException("Unsupported key algorithm: " + profile.keyAlgorithm());
            };
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate certificate key pair", e);
        }
    }

    private KeyPair generateRsa(CertificateProfile profile)
            throws Exception {

        if (!RSA_KEY_SIZES.contains(profile.keySize())) {
            throw new IllegalArgumentException("Unsupported RSA key size: " + profile.keySize());
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

        generator.initialize(profile.keySize(), secureRandom);

        return generator.generateKeyPair();
    }

    private KeyPair generateEc(CertificateProfile profile)
            throws Exception {

        if (!EC_CURVES.contains(profile.ecCurve())) {
            throw new IllegalArgumentException("Unsupported EC curve: " + profile.ecCurve());
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");

        generator.initialize(new ECGenParameterSpec(profile.ecCurve()), secureRandom);

        return generator.generateKeyPair();
    }

}
