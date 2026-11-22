package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.config.security.certificate.CertificateConverter;
import com.atlas.auth.config.security.certificate.CertificateIssueService;
import com.atlas.auth.config.security.certificate.CertificateProperties;
import com.atlas.auth.config.security.certificate.CertificateValidator;
import com.atlas.auth.config.security.certificate.model.CertificateProfile;
import com.atlas.auth.config.security.certificate.model.CertificateSubject;
import com.atlas.auth.config.security.certificate.model.IssuedCertificate;
import com.atlas.auth.domain.dto.CertificateAuthenticateResponse;
import com.atlas.auth.domain.dto.CertificateClientGeneratedRequest;
import com.atlas.auth.domain.dto.CertificateInspectResponse;
import com.atlas.auth.domain.dto.CertificateServerGeneratedRequest;
import com.atlas.auth.enums.CertificateIdentityType;
import com.atlas.common.crypto.digest.DigestUtils;
import com.atlas.common.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/13 13:20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateIssueService certificateIssueService;

    private final CertificateConverter certificateConverter;

    private final CertificateProperties certificateProperties;

    private final CertificateValidator certificateValidator;

    private static final String DEFAULT_PROFILE = "tls-client-rsa";

    private static final String ORGANIZATION = "Atlas";

    private static final String ORGANIZATION_UNIT = "Users";

    // 客户端证书认证
    public CertificateAuthenticateResponse authenticate(String clientCertificate, String clientCertificateChain) throws CertificateException {
        try {
            // 解析客户端证书
            X509Certificate certificate = certificateConverter.fromClientCert(clientCertificate);

            // 解析客户端证书链
            List<X509Certificate> certificateChain = certificateConverter.chainFromClientCert(clientCertificateChain);

            // 校验证书及证书链
            certificateValidator.validate(certificate, certificateChain);

            // 解析证书 Subject
            CertificateSubject subject = getCertificateSubject(certificate);

            // 校验 Subject 并提取用户 ID
            if (!ORGANIZATION.equals(subject.organization()) || !ORGANIZATION_UNIT.equals(subject.organizationalUnit())) {
                throw new CertificateException("Certificate authentication failed");
            }
            Long userId = Long.valueOf(subject.commonName());

            // 计算证书指纹
            String fingerprint = DigestUtils.sha256(certificate.getEncoded());

            return new CertificateAuthenticateResponse(userId, fingerprint);
        } catch (Exception e) {
            log.warn("Certificate authentication failed", e);
            throw new CertificateException("Certificate authentication failed", e);
        }
    }

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

    public CertificateInspectResponse inspect(MultipartFile file, String password) {

        if (file == null || file.isEmpty()) {
            throw new AuthServiceException("Certificate file is required");
        }

        char[] passwordChars = password == null ? new char[0] : password.toCharArray();

        try {
            // 加载 PKCS#12。
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            try (InputStream inputStream = file.getInputStream()) {
                keyStore.load(inputStream, passwordChars);
            } catch (IOException e) {
                throw new AuthServiceException("Failed to load PKCS12, password may be incorrect or file is invalid", e);
            }

            // 查找私钥条目。
            String alias = null;
            PrivateKey privateKey = null;

            Enumeration<String> aliases = keyStore.aliases();

            while (aliases.hasMoreElements()) {

                String currentAlias = aliases.nextElement();

                if (!keyStore.isKeyEntry(currentAlias)) {
                    continue;
                }

                Key key = keyStore.getKey(currentAlias, passwordChars);

                // 排除 SecretKey 条目。
                if (!(key instanceof PrivateKey currentPrivateKey)) {
                    continue;
                }

                if (privateKey != null) {
                    throw new AuthServiceException("PKCS12 contains multiple private key entries");
                }

                alias = currentAlias;
                privateKey = currentPrivateKey;
            }

            if (privateKey == null) {
                throw new AuthServiceException("PKCS12 does not contain a private key");
            }

            // 提取客户端证书。
            Certificate certificateEntry = keyStore.getCertificate(alias);

            if (!(certificateEntry instanceof X509Certificate certificate)) {
                throw new AuthServiceException("PKCS12 does not contain a valid X509 certificate");
            }

            // 提取证书链。
            Certificate[] entries = keyStore.getCertificateChain(alias);

            List<X509Certificate> certificateChain = new ArrayList<>();

            if (entries != null) {
                // 第一张为客户端证书，后续为 CA 证书。
                for (int i = 1; i < entries.length; i++) {

                    if (!(entries[i] instanceof X509Certificate x509Certificate)) {
                        throw new AuthServiceException("PKCS12 contains an invalid certificate chain");
                    }

                    certificateChain.add(x509Certificate);
                }
            }

            // 检查证书有效期。
            try {
                certificate.checkValidity();
            } catch (CertificateException e) {
                throw new AuthServiceException("Certificate is expired or not yet valid", e);
            }

            // 验证 Atlas CA 证书链。
            try {
                certificateValidator.validate(certificate, certificateChain);
            } catch (Exception e) {
                throw new AuthServiceException("Certificate validation failed", e);
            }

            // 解析 Subject。
            CertificateSubject subject = getCertificateSubject(certificate);

            // 验证 Atlas 证书身份规范。
            CertificateIdentityType identityType = resolveIdentityType(subject);

            // 计算证书指纹。
            String fingerprint = DigestUtils.sha256(certificate.getEncoded());

            // 提取 SAN。
            List<String> subjectAlternativeNames = new ArrayList<>();

            Collection<List<?>> sans = certificate.getSubjectAlternativeNames();

            if (sans != null) {
                for (List<?> san : sans) {

                    if (san.size() < 2 || san.get(1) == null) {
                        continue;
                    }

                    if (!(san.get(0) instanceof Integer type)) {
                        continue;
                    }

                    // DNS(2)。
                    if (type == 2) {
                        subjectAlternativeNames.add(san.get(1).toString());
                    }

                    // IP(7)。
                    else if (type == 7) {

                        Object value = san.get(1);

                        if (value instanceof byte[] address) {
                            subjectAlternativeNames.add(InetAddress.getByAddress(address).getHostAddress()
                            );
                        } else {
                            subjectAlternativeNames.add(value.toString());
                        }
                    }
                }
            }

            return new CertificateInspectResponse(
                    identityType,
                    fingerprint,
                    certificate.getSerialNumber().toString(16).toUpperCase(Locale.ROOT),
                    certificate.getSubjectX500Principal().getName(),
                    certificate.getIssuerX500Principal().getName(),
                    certificate.getNotBefore().toInstant(),
                    certificate.getNotAfter().toInstant(),
                    subjectAlternativeNames
            );

        } catch (AuthServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to inspect PKCS12 certificate", e);
            throw new AuthServiceException("Failed to inspect PKCS12 certificate", e);
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    private CertificateIdentityType resolveIdentityType(CertificateSubject subject) {

        if (!ORGANIZATION.equals(subject.organization())) {
            throw new AuthServiceException("Invalid certificate organization");
        }

        if (subject.commonName() == null || subject.commonName().isBlank()) {
            throw new AuthServiceException("Invalid certificate common name");
        }

        if (subject.organizationalUnit() == null) {
            throw new AuthServiceException("Invalid certificate organizational unit");
        }

        return switch (subject.organizationalUnit()) {
            case "Users" -> {
                try {
                    Long.parseLong(subject.commonName());
                } catch (NumberFormatException e) {
                    throw new AuthServiceException("Invalid user certificate subject", e);
                }
                yield CertificateIdentityType.USER;
            }
            case "Devices" -> CertificateIdentityType.DEVICE;
            default -> throw new AuthServiceException("Unsupported certificate identity type");
        };
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
                ORGANIZATION,
                ORGANIZATION_UNIT
        );
    }

    // 从客户端证书中解析 Subject。
    private CertificateSubject getCertificateSubject(X509Certificate certificate) {
        try {
            LdapName ldapName = new LdapName(certificate.getSubjectX500Principal().getName());

            String commonName = null;
            String organization = null;
            String organizationalUnit = null;

            for (Rdn rdn : ldapName.getRdns()) {
                switch (rdn.getType().toUpperCase(Locale.ROOT)) {
                    case "CN" -> commonName = rdn.getValue().toString();
                    case "O" -> organization = rdn.getValue().toString();
                    case "OU" -> organizationalUnit = rdn.getValue().toString();
                }
            }

            return new CertificateSubject(commonName, organization, organizationalUnit);

        } catch (Exception e) {
            throw new AuthServiceException("Failed to parse certificate subject", e);
        }
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
