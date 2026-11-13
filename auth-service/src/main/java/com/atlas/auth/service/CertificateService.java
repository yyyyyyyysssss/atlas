package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.config.security.certificate.CertificateConverter;
import com.atlas.auth.config.security.certificate.CertificateIssueService;
import com.atlas.auth.config.security.certificate.CertificateProperties;
import com.atlas.auth.config.security.certificate.model.CertificateProfile;
import com.atlas.auth.config.security.certificate.model.CertificateSubject;
import com.atlas.auth.config.security.certificate.model.IssuedCertificate;
import com.atlas.auth.domain.dto.CertificateClientGeneratedRequest;
import com.atlas.auth.domain.dto.CertificateServerGeneratedRequest;
import com.atlas.auth.enums.CertificateIdentityType;
import com.atlas.common.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/13 13:20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateIssueService certificateIssueService;

    private final CertificateConverter certificateConverter;

    private final CertificateProperties certificateProperties;

    private static final String DEFAULT_PROFILE = "tls-client-rsa";

    // Atlas 生成密钥对并签发证书，返回 PKCS#12。
    public byte[] serverGenerated(CertificateServerGeneratedRequest request, CertificateIdentityType certificateIdentityType) {

        long start = System.nanoTime();

        CertificateProfile profile = getProfile(DEFAULT_PROFILE);

        CertificateSubject subject = getSubject(certificateIdentityType);

        long issueStart = System.nanoTime();
        IssuedCertificate issuedCertificate = certificateIssueService.issueGenerated(profile, subject);
        long issueEnd = System.nanoTime();

        char[] password = request.p12Password().toCharArray();

        try {
            byte[] pkcs12 = certificateConverter.toPkcs12(
                    "atlas-client",
                    issuedCertificate.privateKey(),
                    issuedCertificate.certificate(),
                    issuedCertificate.certificateChain(),
                    password
            );
            long end = System.nanoTime();

            log.info(
                    "Certificate generation completed: issue={}ms, pkcs12={}ms, total={}ms",
                    (issueEnd - issueStart) / 1_000_000,
                    (end - issueEnd) / 1_000_000,
                    (end - start) / 1_000_000
            );
            return pkcs12;
        } catch (Exception e) {
            log.error("Failed to generate client PKCS12 certificate", e);
            throw new AuthServiceException("Failed to generate PKCS12 certificate", e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    // 客户端提交 CSR，Atlas 验证并签发证书，返回 PEM。
    public byte[] clientGenerated(CertificateClientGeneratedRequest request, CertificateIdentityType certificateIdentityType) {
        CertificateProfile profile = getProfile(DEFAULT_PROFILE);

        CertificateSubject subject = getSubject(certificateIdentityType);

        IssuedCertificate issuedCertificate = certificateIssueService.issueFromCsr(request.csr(), profile, subject);

        try {
            StringBuilder pem = new StringBuilder();

            // 客户端证书
            pem.append(certificateConverter.toPem(issuedCertificate.certificate()));

            // CA 证书链
            if (issuedCertificate.certificateChain() != null && !issuedCertificate.certificateChain().isEmpty()) {

                pem.append(certificateConverter.chainToPem(issuedCertificate.certificateChain()));
            }

            return pem.toString().getBytes(StandardCharsets.US_ASCII);

        } catch (Exception e) {
            log.error("Failed to convert client certificate to PEM", e);
            throw new AuthServiceException("Failed to generate PEM certificate", e);
        }
    }

    // 根据证书身份类型生成 Subject。
    private CertificateSubject getSubject(CertificateIdentityType identityType) {
        return switch (identityType) {
            case USER -> getUserSubject();
            case DEVICE -> throw new AuthServiceException("Device certificate issuance is not supported");
        };
    }

    // 获取当前登录用户的 Subject。
    private CertificateSubject getUserSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthServiceException("User authentication is required");
        }
        if (!(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new AuthServiceException("Invalid authenticated user");
        }
        return new CertificateSubject(
                securityUser.getId().toString(),
                "Atlas",
                "Users"
        );
    }

    // 获取服务端配置的证书 Profile。
    private CertificateProfile getProfile(String profileId) {

        CertificateProperties.Profile config = certificateProperties.profiles().get(profileId);

        if (config == null) {
            throw new AuthServiceException("Certificate profile does not exist: " + profileId);
        }

        return new CertificateProfile(
                profileId,
                config.keyAlgorithm(),
                config.keySize(),
                config.ecCurve(),
                config.validityDays()
        );
    }

}
