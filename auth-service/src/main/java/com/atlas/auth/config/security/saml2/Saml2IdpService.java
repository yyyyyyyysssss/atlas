package com.atlas.auth.config.security.saml2;

import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.atlas.auth.service.Saml2RegisteredClientService;
import com.atlas.common.crypto.random.SecureRandomUtils;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.common.security.model.SecurityUser;
import com.atlas.common.security.properties.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.*;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.IDPSSODescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.KeyDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.SingleSignOnServiceBuilder;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.impl.KeyInfoBuilder;
import org.opensaml.xmlsec.signature.impl.X509CertificateBuilder;
import org.opensaml.xmlsec.signature.impl.X509DataBuilder;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/8 14:38
 */
@Component
@Slf4j
public class Saml2IdpService {

    private final SecurityProperties securityProperties;

    private final Saml2RegisteredClientService saml2RegisteredClientService;

    private final RedisHelper redisHelper;

    /**
     * IdP 签名证书。
     */
    private final X509Certificate signingCertificate;

    /**
     * OpenSAML 签名凭据。
     */
    private final Credential signingCredential;

    private static final long RESPONSE_VALIDITY_SECONDS = 300;

    // 防止超大 XML 被解析1 最大限制1MB
    private static final int MAX_SAML_XML_BYTES = 1024 * 1024;// 1 MiB

    // 限制HTTP-Redirect 解码后的压缩数据的大小，避免处理异常大的请求
    private static final int MAX_REDIRECT_REQUEST_BYTES = 256 * 1024; // 256 KiB

    // 在 Base64 解码之前就拦截超长输入，减少不必要的内存分配
    private static final int MAX_SAML_REQUEST_LENGTH = 1_400_000;

    private static final String SAML_REQUEST_REPLAY_KEY = "saml2:idp:replay:";

    private static final Map<String, String> REDIRECT_SIGNATURE_ALGORITHMS = Map.of(
            "http://www.w3.org/2000/09/xmldsig#rsa-sha1", "SHA1withRSA",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", "SHA256withRSA",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384", "SHA384withRSA",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", "SHA512withRSA"
    );

    private static final Set<String> SUPPORTED_RESPONSE_BINDINGS = Set.of(
            SAMLConstants.SAML2_POST_BINDING_URI,
            SAMLConstants.SAML2_REDIRECT_BINDING_URI
    );

    public Saml2IdpService(SecurityProperties securityProperties,
                           Saml2RegisteredClientService saml2RegisteredClientService,
                           Saml2SigningCredentialLoader saml2SigningCredentialLoader,
                           RedisHelper redisHelper) {
        this.securityProperties = securityProperties;
        this.saml2RegisteredClientService = saml2RegisteredClientService;
        this.redisHelper = redisHelper;

        SecurityProperties.SigningConfig config = securityProperties.getSaml2().getIdp().getSigning();
        String keyStoreLocation = config.getKeyStore();
        String keyStorePassword = config.getKeyStorePassword();
        String keyAlias = config.getKeyAlias();

        Saml2SigningCredentialLoader.Saml2SigningCredential saml2SigningCredential = saml2SigningCredentialLoader.load(keyStoreLocation, keyStorePassword, keyAlias);

        this.signingCertificate = saml2SigningCredential.certificate();
        this.signingCredential = saml2SigningCredential.toOpenSamlCredential();

    }

    static {
        OpenSamlInitializationService.initialize();
    }

    // 生成 SAML 2.0 IdP Metadata。
    public String metadata() {
        SecurityProperties.IdpConfig idp = securityProperties.getSaml2().getIdp();
        EntityDescriptor entityDescriptor = buildMetadata(idp, signingCertificate);
        try {
            XMLObjectSupport.marshall(entityDescriptor);
            Node node = entityDescriptor.getDOM();
            if (node == null) {
                throw new IllegalStateException("SAML IdP metadata DOM is null after marshalling");
            }
            return SerializeSupport.nodeToString(node);
        } catch (MarshallingException e) {
            throw new IllegalStateException("Failed to generate SAML 2.0 IdP metadata", e);
        }
    }

    public String sso(String samlRequest, Saml2MessageBinding binding, String rawQueryString) {
        if (samlRequest == null || samlRequest.isBlank()) {
            throw invalidRequest("invalid SAMLRequest");
        }
        if (samlRequest.length() > MAX_SAML_REQUEST_LENGTH) {
            throw invalidRequest("SAMLRequest exceeds the maximum allowed length");
        }
        if (binding == null) {
            throw invalidRequest("invalid SAML binding");
        }
        log.info("SAML SSO request received, binding={}", binding.name());
        // 解析 AuthnRequest
        AuthnRequest authnRequest = authnRequest(samlRequest, binding);
        if (!SAMLVersion.VERSION_20.equals(authnRequest.getVersion())) {
            throw invalidRequest("Unsupported SAML protocol version");
        }
        // 校验响应方式
        String protocolBinding = authnRequest.getProtocolBinding();
        if (protocolBinding != null && !SUPPORTED_RESPONSE_BINDINGS.contains(protocolBinding)) {
            throw invalidRequest("Unsupported SAML Response binding");
        }
        // 校验 AuthnRequest 的时间，防止接受过期请求
        if (authnRequest.getIssueInstant() == null) {
            throw invalidRequest("AuthnRequest IssueInstant is required");
        }
        Instant issueInstant = authnRequest.getIssueInstant();
        Instant now = Instant.now();
        // 最多允许 1 分钟的未来时钟偏差
        if (issueInstant.isAfter(now.plusSeconds(60))) {
            throw invalidRequest("AuthnRequest IssueInstant is in the future");
        }
        // 请求最多允许存在 5 分钟
        if (issueInstant.isBefore(now.minusSeconds(300))) {
            throw invalidRequest("AuthnRequest IssueInstant has expired");
        }

        String requestId = authnRequest.getID();
        if (requestId == null || requestId.isBlank()) {
            throw invalidRequest("invalid AuthnRequest ID");
        }

        String issuer = authnRequest.getIssuer() != null ? authnRequest.getIssuer().getValue() : null;
        if (issuer == null || issuer.isBlank()) {
            throw invalidRequest("invalid AuthnRequest Issuer");
        }

        // 校验 Destination
        String destination = authnRequest.getDestination();
        String expectedDestination = securityProperties.getSaml2().getIdp().getSsoUrl();
        if (destination == null || destination.isBlank()) {
            throw invalidRequest("AuthnRequest Destination is required");
        }
        if (!expectedDestination.equals(destination)) {
            throw invalidRequest("Invalid AuthnRequest Destination");
        }

        // 当前仅支持通过已注册的 ACS URL 确定响应地址
        if (authnRequest.getAssertionConsumerServiceIndex() != null) {
            throw invalidRequest("AssertionConsumerServiceIndex is not supported");
        }

        // 根据 Issuer 查询已注册的 SP
        Saml2RegisteredClient saml2RegisteredClient = saml2RegisteredClientService.getEnabledByEntityId(issuer);
        if (saml2RegisteredClient == null) {
            throw invalidRequest("Unregistered SAML Service Provider");
        }
        // 校验 Entity ID
        if (saml2RegisteredClient.getEntityId() == null || !issuer.equals(saml2RegisteredClient.getEntityId())) {
            throw invalidRequest("SAML Service Provider Entity ID mismatch");
        }
        String acsUrl = authnRequest.getAssertionConsumerServiceURL();
        // 校验 ACS 配置
        String registeredAcsUrl = saml2RegisteredClient.getAcsUrl();
        if (registeredAcsUrl == null || registeredAcsUrl.isBlank()) {
            throw invalidRequest("SAML Service Provider ACS URL is not configured");
        }
        if (acsUrl != null && !acsUrl.isBlank()) {
            if (!registeredAcsUrl.equals(acsUrl)) {
                throw invalidRequest("Unregistered AssertionConsumerServiceURL");
            }
        }
        // 校验签名策略及证书配置
        boolean requireSignature = Boolean.TRUE.equals(saml2RegisteredClient.getRequireSignedAuthnRequest());
        // 实际签名是否存在
        boolean signaturePresent;
        // 是否已完成签名验证
        boolean signatureVerified = false;
        // 按请求绑定方式判断签名是否存在
        Map<String, String> params = null;
        if (Saml2MessageBinding.REDIRECT.equals(binding)) {
            params = parseRawQueryString(rawQueryString);
            // Redirect 绑定使用查询参数签名
            boolean hasSigAlg = params.containsKey("SigAlg");
            boolean hasSignature = params.containsKey("Signature");
            // 只要出现任意一个签名参数，就视为尝试签名
            // 避免攻击者只传入部分签名参数而绕过检查
            if (hasSigAlg != hasSignature) {
                throw invalidRequest("Incomplete Redirect signature parameters");
            }
            signaturePresent = hasSignature;
        } else if (Saml2MessageBinding.POST.equals(binding)) {
            // POST 绑定通过 XML 内部的 ds:Signature 签名
            signaturePresent = authnRequest.getSignature() != null;
        } else {
            throw invalidRequest("Unsupported SAML binding");
        }
        // 要求签名时，如果请求没有携带签名，直接拒绝
        if (requireSignature && !signaturePresent) {
            throw invalidRequest("AuthnRequest signature is required");
        }
        // 只要实际携带签名，就必须验证 不受 requireSignedAuthnRequest 配置影响
        if (signaturePresent) {
            X509Certificate x509Certificate = parseCertificate(saml2RegisteredClient.getVerificationCertificate());
            if (x509Certificate == null) {
                throw invalidRequest("SP verification certificate is not configured");
            }
            if (Saml2MessageBinding.REDIRECT.equals(binding)) {
                verifyRedirectSignature(
                        params.get("SAMLRequest"),
                        params.get("SigAlg"),
                        params.get("Signature"),
                        params.get("RelayState"),
                        x509Certificate);
            } else {
                verifyPostSignature(authnRequest, x509Certificate);
            }
            signatureVerified = true;
        }
        // 防重放
        if (!markRequestAsSeen(issuer, requestId)){
            throw invalidRequest("Duplicate AuthnRequest");
        }
        log.info("[SAML2 IdP] AuthnRequest validation completed, requestId={}, issuer={}, binding={}, destination={}, acs={} signatureRequired={}, signatureVerified={}",
                requestId,
                issuer,
                binding.name(),
                destination,
                acsUrl,
                requireSignature,
                signatureVerified
        );
        return null;
    }

    public AuthnRequest authnRequest(String samlRequest, Saml2MessageBinding binding) {
        // 校验
        if (samlRequest == null || samlRequest.isBlank()) {
            throw invalidRequest("SAMLRequest must not be empty");
        }
        if (binding == null) {
            throw invalidRequest("SAML binding must not be null");
        }
        boolean redirect = Saml2MessageBinding.REDIRECT.equals(binding);
        boolean post = Saml2MessageBinding.POST.equals(binding);
        if (!redirect && !post) {
            throw invalidRequest("Unsupported SAML binding");
        }
        // 第一步：Base64 解码
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(samlRequest);
        } catch (IllegalArgumentException e) {
            throw invalidRequest("SAMLRequest is not valid Base64");
        }
        if (decoded.length == 0) {
            throw invalidRequest("SAMLRequest must not be empty");
        }
        byte[] xmlBytes;
        if (redirect) {
            if (decoded.length > MAX_REDIRECT_REQUEST_BYTES) {
                throw invalidRequest("Compressed SAMLRequest exceeds the maximum allowed size");
            }
            xmlBytes = inflateRawDeflate(decoded, MAX_SAML_XML_BYTES);
        } else {
            if (decoded.length > MAX_SAML_XML_BYTES) {
                throw invalidRequest("SAML XML exceeds the maximum allowed size");
            }
            xmlBytes = decoded;
        }
        // 第二步：解析 XML
        ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        if (parserPool == null) {
            throw new IllegalStateException("OpenSAML XML ParserPool has not been initialized");
        }
        final Element element;
        try (InputStream input = new ByteArrayInputStream(xmlBytes)) {
            element = parserPool.parse(input).getDocumentElement();
        } catch (XMLParserException | IOException e) {
            throw invalidRequest("Failed to parse SAML XML");
        }
        if (element == null) {
            throw invalidRequest("SAMLRequest contains an empty XML document");
        }
        // 通过 OpenSAML 转换为 XMLObject
        UnmarshallerFactory factory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        Unmarshaller unmarshaller = factory.getUnmarshaller(element);
        if (unmarshaller == null) {
            throw invalidRequest("Unsupported SAML XML element");
        }
        final XMLObject xmlObject;
        try {
            xmlObject = unmarshaller.unmarshall(element);
        } catch (UnmarshallingException e) {
            throw invalidRequest("Failed to unmarshal SAML XML");
        }
        // 确认根对象确实是 AuthnRequest
        if (!(xmlObject instanceof AuthnRequest authnRequest)) {
            throw invalidRequest("SAMLRequest root element is not AuthnRequest");
        }
        return authnRequest;
    }

    public Response response(AuthnRequest authnRequest, Saml2RegisteredClient registeredClient, Authentication authentication) {
        Objects.requireNonNull(authnRequest, "authnRequest");
        Objects.requireNonNull(registeredClient, "registeredClient");
        Objects.requireNonNull(authentication, "authentication");
        if (!authentication.isAuthenticated()) {
            throw new BadCredentialsException("BadCredentials");
        }
        String entityId = securityProperties.getSaml2().getIdp().getEntityId();
        String requestId = authnRequest.getID();
        String acsUrl = registeredClient.getAcsUrl();
        String audience = registeredClient.getEntityId();


        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Long userId = securityUser.getId();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(RESPONSE_VALIDITY_SECONDS);

        // 构建 Response
        Response response = build(Response.DEFAULT_ELEMENT_NAME);
        response.setID(SecureRandomUtils.generate());
        response.setVersion(SAMLVersion.VERSION_20);
        response.setIssueInstant(now);
        response.setDestination(acsUrl);
        response.setInResponseTo(requestId);

        // 设置 IdP Issuer
        Issuer responseIssuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        responseIssuer.setValue(entityId);
        response.setIssuer(responseIssuer);

        // 设置成功状态
        Status status = build(Status.DEFAULT_ELEMENT_NAME);
        StatusCode statusCode = build(StatusCode.DEFAULT_ELEMENT_NAME);
        statusCode.setValue(StatusCode.SUCCESS);
        status.setStatusCode(statusCode);
        response.setStatus(status);

        // 构建 Assertion
        Assertion assertion = build(Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID(SecureRandomUtils.generate());
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssueInstant(now);
        Issuer assertionIssuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        assertionIssuer.setValue(entityId);
        assertion.setIssuer(assertionIssuer);

        // Subject / NameID
        Subject subject = build(Subject.DEFAULT_ELEMENT_NAME);
        NameID nameID = build(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setFormat(NameID.UNSPECIFIED);
        nameID.setValue(userId.toString());
        subject.setNameID(nameID);

        // SubjectConfirmation：限制接收方、请求 ID 和有效期
        SubjectConfirmation confirmation = build(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        confirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        SubjectConfirmationData confirmationData = build(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        confirmationData.setRecipient(acsUrl);
        confirmationData.setInResponseTo(requestId);
        confirmationData.setNotOnOrAfter(expiresAt);
        confirmation.setSubjectConfirmationData(confirmationData);
        subject.getSubjectConfirmations().add(confirmation);
        assertion.setSubject(subject);

        // 设置用户信息
        AttributeStatement attributeStatement = build(AttributeStatement.DEFAULT_ELEMENT_NAME);
        addAttribute(attributeStatement, "sub", userId);
        addAttribute(attributeStatement, "email", securityUser.getEmail());
        addAttribute(attributeStatement, "avatar", securityUser.getAvatar());
        addAttribute(attributeStatement, "fullName", securityUser.getFullName());
        addAttribute(attributeStatement, "emailVerified", true);
        assertion.getAttributeStatements().add(attributeStatement);

        // Conditions：限定断言有效期和目标 SP
        Conditions conditions = build(Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(now.minusSeconds(30));
        conditions.setNotOnOrAfter(expiresAt);
        AudienceRestriction audienceRestriction = build(AudienceRestriction.DEFAULT_ELEMENT_NAME);
        Audience audienceElement = build(Audience.DEFAULT_ELEMENT_NAME);
        audienceElement.setURI(audience);
        audienceRestriction.getAudiences().add(audienceElement);
        conditions.getAudienceRestrictions().add(audienceRestriction);
        assertion.setConditions(conditions);

        // AuthnStatement / AuthnContext
        AuthnStatement authnStatement = build(AuthnStatement.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnInstant(now);
        authnStatement.setSessionIndex(SecureRandomUtils.generate());
        authnStatement.setSessionNotOnOrAfter(expiresAt);
        AuthnContext authnContext = build(AuthnContext.DEFAULT_ELEMENT_NAME);
        AuthnContextClassRef classRef = build(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);

        // 不虚构密码、Passkey 等具体认证方式
        classRef.setURI("urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified");
        authnContext.setAuthnContextClassRef(classRef);
        authnStatement.setAuthnContext(authnContext);
        assertion.getAuthnStatements().add(authnStatement);

        // 根据 SP 配置决定是否对 Assertion 签名
        if (Boolean.TRUE.equals(registeredClient.getSignAssertion())) {
            sign(assertion);
        }

        // 将 Assertion 放入 Response
        response.getAssertions().add(assertion);

        // 根据 SP 配置决定是否对 Response 签名
        if (Boolean.TRUE.equals(registeredClient.getSignResponse())) {
            sign(response);
        }

        return response;
    }

    public String serializeResponse(Response response) {
        try {
            XMLObjectSupport.marshall(response);
            Node node = response.getDOM();
            if (node == null) {
                throw invalidResponse("SAML Response DOM is null after marshalling");
            }
            return SerializeSupport.nodeToString(node);
        } catch (MarshallingException e) {
            throw new IllegalStateException("Failed to serialize SAML 2.0 Response", e);
        }
    }


    private byte[] inflateRawDeflate(byte[] compressed, int maxOutputBytes) {
        if (compressed == null || compressed.length == 0) {
            throw invalidRequest("Compressed SAMLRequest must not be empty");
        }
        if (maxOutputBytes <= 0) {
            throw new IllegalArgumentException("maxOutputBytes must be greater than zero");
        }
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(compressed);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            while (!inflater.finished()) {
                final int count;
                try {
                    count = inflater.inflate(buffer);
                } catch (DataFormatException e) {
                    throw invalidRequest("Invalid DEFLATE data in SAMLRequest");
                }
                if (count > 0) {
                    // 防止 total + count 发生整数溢出。
                    if (count > maxOutputBytes - total) {
                        throw invalidRequest("Decompressed SAMLRequest exceeds the maximum allowed size");
                    }
                    output.write(buffer, 0, count);
                    total += count;
                    continue;
                }
                if (inflater.needsDictionary()) {
                    throw invalidRequest("SAMLRequest DEFLATE stream requires a dictionary");
                }
                if (inflater.needsInput()) {
                    throw invalidRequest("Incomplete DEFLATE stream in SAMLRequest");
                }
                // 没有输出、没有结束、也不需要更多输入，
                // 说明解压器无法继续正常推进。
                throw invalidRequest("Unable to make progress while decompressing SAMLRequest");
            }

            // 输入一次性提供给 Inflater，因此可以检查整个输入数组中
            // 是否还有未消费的数据。
            if (inflater.getRemaining() != 0) {
                throw invalidRequest("Unexpected trailing data in DEFLATE stream");
            }
            if (total == 0) {
                throw invalidRequest("Decompressed SAMLRequest must not be empty");
            }
            return output.toByteArray();
        } finally {
            inflater.end();
        }
    }

    // 对 SAML 对象签名。
    private void sign(XMLObject object) {
        Signature signature = build(Signature.DEFAULT_ELEMENT_NAME);
        signature.setSigningCredential(signingCredential);

        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        if (object instanceof Assertion assertion) {
            assertion.setSignature(signature);
        } else if (object instanceof Response response) {
            response.setSignature(signature);
        } else {
            throw invalidResponse("Unsupported SAML object: " + object.getClass().getName());
        }
        try {
            Marshaller marshaller = XMLObjectProviderRegistrySupport
                    .getMarshallerFactory()
                    .getMarshaller(object);

            if (marshaller == null) {
                throw invalidResponse("No marshaller registered for SAML object");
            }
            marshaller.marshall(object);
            Signer.signObject(signature);
        } catch (MarshallingException | org.opensaml.xmlsec.signature.support.SignatureException e) {
            throw invalidResponse("Failed to sign SAML object: " + e.getMessage());
        }
    }

    private void addAttribute(AttributeStatement statement, String name, Object value) {
        // 不返回 null 属性
        if (value == null) {
            return;
        }
        Attribute attribute = build(Attribute.DEFAULT_ELEMENT_NAME);
        attribute.setName(name);
        XSAny attributeValue = build(org.opensaml.core.xml.schema.XSAny.TYPE_NAME);
        attributeValue.setTextContent(value.toString());
        attribute.getAttributeValues().add(attributeValue);
        statement.getAttributes().add(attribute);
    }

    @SuppressWarnings("unchecked")
    private <T extends XMLObject> T build(QName name) {
        XMLObjectBuilderFactory factory = XMLObjectProviderRegistrySupport.getBuilderFactory();

        XMLObjectBuilder<?> builder = factory.getBuilder(name);
        if (builder == null) {
            throw invalidResponse("No OpenSAML builder registered for " + name);
        }

        return (T) builder.buildObject(name);
    }

    private EntityDescriptor buildMetadata(SecurityProperties.IdpConfig idp, X509Certificate certificate) {
        // <md:EntityDescriptor>
        EntityDescriptor entityDescriptor = new EntityDescriptorBuilder().buildObject();
        entityDescriptor.setEntityID(idp.getEntityId());

        // <md:IDPSSODescriptor>
        IDPSSODescriptor idpDescriptor = new IDPSSODescriptorBuilder().buildObject();

        // SAML 2.0 Protocol
        idpDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
        // 当前阶段不要求 SP 对 AuthnRequest 签名。 后续如果 Atlas 要求所有 AuthnRequest 必须由 SP 签名，这里改成 true。
        idpDescriptor.setWantAuthnRequestsSigned(true);
        // Signing Certificate
        KeyDescriptor keyDescriptor = new KeyDescriptorBuilder().buildObject();
        keyDescriptor.setUse(UsageType.SIGNING);
        keyDescriptor.setKeyInfo(buildKeyInfo(certificate));

        idpDescriptor.getKeyDescriptors().add(keyDescriptor);

        // HTTP-Redirect Binding
        SingleSignOnService redirectService = new SingleSignOnServiceBuilder().buildObject();
        redirectService.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        redirectService.setLocation(idp.getSsoUrl());

        idpDescriptor.getSingleSignOnServices().add(redirectService);

        // HTTP-POST Binding
        SingleSignOnService postService = new SingleSignOnServiceBuilder().buildObject();
        postService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        postService.setLocation(idp.getSsoUrl());

        idpDescriptor.getSingleSignOnServices().add(postService);

        /*
         * EntityDescriptor
         *     └── IDPSSODescriptor
         */
        entityDescriptor.getRoleDescriptors().add(idpDescriptor);

        return entityDescriptor;
    }

    /**
     * 构建：
     *
     * <pre>
     * &lt;ds:KeyInfo&gt;
     *     &lt;ds:X509Data&gt;
     *         &lt;ds:X509Certificate&gt;
     *             Base64(DER Certificate)
     *         &lt;/ds:X509Certificate&gt;
     *     &lt;/ds:X509Data&gt;
     * &lt;/ds:KeyInfo&gt;
     * </pre>
     */
    private KeyInfo buildKeyInfo(X509Certificate certificate) {
        KeyInfo keyInfo = new KeyInfoBuilder().buildObject();

        X509Data x509Data = new X509DataBuilder().buildObject();

        org.opensaml.xmlsec.signature.X509Certificate samlCertificate = new X509CertificateBuilder().buildObject();

        try {
            /*
             * Java X509Certificate
             *
             *      ↓ getEncoded()
             *
             * DER 二进制证书
             */
            byte[] derCertificate = certificate.getEncoded();

            /*
             * DER
             *
             *      ↓ Base64
             *
             * SAML Metadata 中的
             * <ds:X509Certificate>
             */
            String base64Certificate = Base64.getEncoder().encodeToString(derCertificate);

            samlCertificate.setValue(base64Certificate);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode SAML IdP X.509 certificate", e);
        }

        x509Data.getX509Certificates().add(samlCertificate);

        keyInfo.getX509Datas().add(x509Data);

        return keyInfo;
    }


    private void verifyRedirectSignature(String samlRequest, String sigAlg, String signature, String relayState, X509Certificate certificate) {
        if (samlRequest == null || samlRequest.isBlank()) {
            throw invalidRequest("SAMLRequest must not be empty");
        }
        if (sigAlg == null || sigAlg.isBlank()) {
            throw invalidRequest("SigAlg must not be empty");
        }
        if (signature == null || signature.isBlank()) {
            throw invalidRequest("Signature must not be empty");
        }
        if (certificate == null) {
            throw invalidRequest("SP signing certificate must not be empty");
        }
        String decodedSigAlg = URLDecoder.decode(sigAlg, StandardCharsets.UTF_8);
        String decodedSignature = URLDecoder.decode(signature, StandardCharsets.UTF_8);

        String jcaAlgorithm = REDIRECT_SIGNATURE_ALGORITHMS.get(decodedSigAlg);
        if (jcaAlgorithm == null) {
            throw invalidRequest("Unsupported signature algorithm");
        }
        try {
            StringBuilder signedQuery = new StringBuilder()
                    .append("SAMLRequest=")
                    .append(samlRequest);
            if (relayState != null) {
                signedQuery.append("&RelayState=").append(relayState);
            }
            signedQuery.append("&SigAlg=").append(sigAlg);
            byte[] signedBytes = signedQuery.toString().getBytes(StandardCharsets.UTF_8);
            byte[] signatureBytes = Base64.getDecoder().decode(decodedSignature);
            java.security.Signature verifier = java.security.Signature.getInstance(jcaAlgorithm);
            verifier.initVerify(certificate.getPublicKey());
            verifier.update(signedBytes);
            if (!verifier.verify(signatureBytes)) {
                throw invalidRequest("Invalid REDIRECT request signature");
            }
        } catch (IllegalArgumentException e) {
            throw invalidRequest("Invalid REDIRECT request signature encoding");
        } catch (java.security.GeneralSecurityException e) {
            throw invalidRequest("Failed to verify REDIRECT request signature");
        }
    }

    private Map<String, String> parseRawQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isBlank()) {
            throw invalidRequest("Missing query string");
        }
        for (String pair : queryString.split("&")) {
            int index = pair.indexOf('=');
            if (index <= 0) {
                continue;
            }
            String key = pair.substring(0, index);
            String value = pair.substring(index + 1);
            if (params.putIfAbsent(key, value) != null) {
                throw invalidRequest("Duplicate SAML query parameter");
            }
        }
        return params;
    }

    private void verifyPostSignature(AuthnRequest authnRequest, X509Certificate certificate) {
        if (authnRequest == null) {
            throw invalidRequest("AuthnRequest must not be null");
        }
        if (certificate == null) {
            throw invalidRequest("SP signing certificate must not be empty");
        }
        if (authnRequest.getSignature() == null) {
            throw invalidRequest("POST AuthnRequest signature is missing");
        }
        try {
            // 校验 SAML XML Signature Profile
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(authnRequest.getSignature());
            // 使用 SP 注册的 X.509 证书验证 XML 数字签名
            BasicX509Credential credential = new BasicX509Credential(certificate);
            SignatureValidator.validate(authnRequest.getSignature(), credential);
        } catch (SignatureException e) {
            throw invalidRequest("Invalid POST request signature");
        }
    }

    /**
     * 将配置中的 PEM 格式证书转换为 Java X509Certificate。
     *
     * <p>配置格式：
     *
     * <pre>
     * -----BEGIN CERTIFICATE-----
     * MIID...
     * -----END CERTIFICATE-----
     * </pre>
     */
    private X509Certificate parseCertificate(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("SAML IdP signing certificate is not configured");
        }
        try {
            String base64Certificate = pem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");

            byte[] derCertificate = Base64.getDecoder().decode(base64Certificate);

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(derCertificate));

        } catch (Exception e) {
            throw invalidRequest("Invalid SAML IdP X.509 certificate");
        }
    }

    private boolean markRequestAsSeen(String issuer, String requestId) {
        return redisHelper.setIfAbsent(SAML_REQUEST_REPLAY_KEY + ":" + issuer + ":" + requestId, "1", Duration.ofMinutes(5));
    }

    public static Saml2AuthenticationException invalidRequest(String description) {

        return new Saml2AuthenticationException(new Saml2Error("urn:oasis:names:tc:SAML:2.0:status:Requester", description));
    }

    public static Saml2AuthenticationException invalidResponse(String description) {

        return new Saml2AuthenticationException(new Saml2Error("urn:oasis:names:tc:SAML:2.0:status:Response", description));
    }

}
