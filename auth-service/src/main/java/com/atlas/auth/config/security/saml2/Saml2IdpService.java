package com.atlas.auth.config.security.saml2;

import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.atlas.auth.service.Saml2RegisteredClientService;
import com.atlas.common.crypto.random.SecureRandomUtils;
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
import org.opensaml.core.xml.schema.XSBoolean;
import org.opensaml.core.xml.schema.XSBooleanValue;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSBooleanBuilder;
import org.opensaml.core.xml.schema.impl.XSStringBuilder;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.core.impl.ResponseBuilder;
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
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
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

    private static final Map<String, String> REDIRECT_SIGNATURE_ALGORITHMS = Map.of(
            "http://www.w3.org/2000/09/xmldsig#rsa-sha1", "SHA1withRSA",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", "SHA256withRSA",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384", "SHA384withRSA",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", "SHA512withRSA"
    );

    public Saml2IdpService(SecurityProperties securityProperties, Saml2SigningCredentialLoader saml2SigningCredentialLoader, Saml2RegisteredClientService saml2RegisteredClientService) {
        this.securityProperties = securityProperties;
        this.saml2RegisteredClientService = saml2RegisteredClientService;
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

    public String createResponse(Saml2IdpRequestContext requestContext, Authentication authentication) {
        Objects.requireNonNull(requestContext, "requestContext");
        Objects.requireNonNull(authentication, "authentication");
        if (!authentication.isAuthenticated()) {
            throw new BadCredentialsException("BadCredentials");
        }
        Saml2RegisteredClient registeredClient = saml2RegisteredClientService.getById(requestContext.getRegisteredClientId());
        Objects.requireNonNull(registeredClient, "registeredClient");
        String entityId = securityProperties.getSaml2().getIdp().getEntityId();
        String requestId = requestContext.getRequestId();
        String acsUrl = requestContext.getAcsUrl();
        String audience = registeredClient.getEntityId();
        String nameIdFormat = requestContext.getNameIdFormat();


        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Long userId = securityUser.getId();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(RESPONSE_VALIDITY_SECONDS);

        // 构建 Response
        Response response = build(Response.DEFAULT_ELEMENT_NAME);
        response.setID(generateSamlId());
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
        assertion.setID(generateSamlId());
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssueInstant(now);
        Issuer assertionIssuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        assertionIssuer.setValue(entityId);
        assertion.setIssuer(assertionIssuer);

        // Subject / NameID
        Subject subject = build(Subject.DEFAULT_ELEMENT_NAME);
        NameID nameID = build(NameID.DEFAULT_ELEMENT_NAME);
        if (nameIdFormat == null || nameIdFormat.isBlank()) {
            nameIdFormat = NameID.UNSPECIFIED;
        }
        nameID.setFormat(nameIdFormat);
        nameID.setFormat(requestContext.getNameIdFormat());
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
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("sub", userId);
        attributes.put("email", securityUser.getEmail());
        attributes.put("avatar", securityUser.getAvatar());
        attributes.put("fullName", securityUser.getFullName());
        attributes.put("emailVerified", true);
        AttributeStatement attributeStatement = build(AttributeStatement.DEFAULT_ELEMENT_NAME);
        Map<String,String> mappings = Optional.ofNullable(registeredClient.getAttributeMappings()).orElse(Collections.emptyMap());
        attributes.forEach((key, value) -> {
            String attributeName = mappings.getOrDefault(key, key);
            addAttribute(attributeStatement, attributeName, value);
        });
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
        authnStatement.setSessionIndex(generateSamlId());
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

        return serializeResponseXml(response);
    }

    public String createErrorResponse(Saml2IdpAuthenticationException exception){
        Response response = new ResponseBuilder().buildObject();
        response.setID("_" + SecureRandomUtils.generate());
        response.setIssueInstant(Instant.now());
        response.setVersion(SAMLVersion.VERSION_20);
        // 设置 IdP Issuer
        Issuer responseIssuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        responseIssuer.setValue(securityProperties.getSaml2().getIdp().getEntityId());
        response.setIssuer(responseIssuer);
        // 设置失败状态
        Status status = build(Status.DEFAULT_ELEMENT_NAME);
        StatusCode statusCode = build(StatusCode.DEFAULT_ELEMENT_NAME);
        statusCode.setValue(resolveStatusCode(exception.getSaml2Error()));
        status.setStatusCode(statusCode);
        String message = exception.getSaml2Error().getDescription();
        if (message != null) {
            StatusMessage statusMessage = build(StatusMessage.DEFAULT_ELEMENT_NAME);
            statusMessage.setValue(message);
            status.setStatusMessage(statusMessage);
        }
        response.setStatus(status);

        AuthnRequest authnRequest = exception.getAuthnRequest();
        if (authnRequest != null){
            response.setInResponseTo(authnRequest.getID());
            String acsUrl = authnRequest.getAssertionConsumerServiceURL();
            if (acsUrl != null && !acsUrl.isBlank()) {
                response.setDestination(acsUrl);
            } else if (authnRequest.getDestination() != null) {
                response.setDestination(authnRequest.getDestination());
            }
        }

        return serializeResponseXml(response);
    }

    private String serializeResponseXml(Response response) {
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

    public Saml2IdpResponse createRedirectResponse(Saml2IdpRequestContext requestContext,Authentication authentication){
        String samlResponse = createResponse(requestContext, authentication);
        String encodedResponse = encodeRedirect(samlResponse);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(requestContext.getAcsUrl())
                .queryParam("SAMLResponse", encodedResponse);
        String relayState = requestContext.getRelayState();
        if (relayState != null && !relayState.isBlank()) {
            builder.queryParam("RelayState", URLEncoder.encode(relayState, StandardCharsets.UTF_8));
        }
        return Saml2IdpResponse.builder()
                .binding(Saml2MessageBinding.REDIRECT.name())
                .redirectUrl(builder.build(true).toUriString())
                .build();
    }

    public Saml2IdpResponse createPostResponse(Saml2IdpRequestContext requestContext, Authentication authentication) {
        String samlResponse = createResponse(requestContext, authentication);
        String encodedResponse = Base64.getEncoder().encodeToString(samlResponse.getBytes(StandardCharsets.UTF_8));
        return Saml2IdpResponse.builder()
                .binding(Saml2MessageBinding.POST.name())
                .acsUrl(requestContext.getAcsUrl())
                .samlResponse(encodedResponse)
                .relayState(requestContext.getRelayState())
                .build();
    }

    private String resolveStatusCode(Saml2Error error) {

        return switch (error.getErrorCode()) {
            // 请求方错误
            case Saml2ErrorCodes.INVALID_REQUEST,
                 Saml2ErrorCodes.MALFORMED_REQUEST_DATA,
                 Saml2ErrorCodes.INVALID_DESTINATION,
                 Saml2ErrorCodes.INVALID_SIGNATURE,
                 Saml2ErrorCodes.INVALID_ISSUER,
                 Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                 Saml2ErrorCodes.INVALID_IN_RESPONSE_TO -> StatusCode.REQUESTER;

            // IdP内部错误
            case Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                 Saml2ErrorCodes.DECRYPTION_ERROR -> StatusCode.RESPONDER;

            default -> StatusCode.RESPONDER;
        };
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

    private String encodeRedirect(String xml) {
        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        deflater.setInput(xmlBytes);
        deflater.finish();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        deflater.end();
        byte[] compressed = outputStream.toByteArray();
        return URLEncoder.encode(Base64.getEncoder().encodeToString(compressed), StandardCharsets.UTF_8);
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


        if (value instanceof Boolean bool) {
            XSBooleanBuilder booleanBuilder = (XSBooleanBuilder) XMLObjectProviderRegistrySupport
                            .getBuilderFactory()
                            .getBuilder(XSBoolean.TYPE_NAME);
            if (booleanBuilder == null) {
                throw new IllegalStateException("OpenSAML XSString builder not initialized");
            }
            XSBoolean booleanValue = booleanBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSBoolean.TYPE_NAME);
            booleanValue.setValue(new XSBooleanValue(bool, false));
            attribute.getAttributeValues().add(booleanValue);

        } else {
            XSStringBuilder stringBuilder = (XSStringBuilder) XMLObjectProviderRegistrySupport
                            .getBuilderFactory()
                            .getBuilder(XSString.TYPE_NAME);
            if (stringBuilder == null) {
                throw new IllegalStateException("OpenSAML XSString builder not initialized");
            }
            XSString stringValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            stringValue.setValue(String.valueOf(value));
            attribute.getAttributeValues().add(stringValue);
        }

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


    public void verifyRedirectSignature(Map<String, String> requestParams, String verificationCertificate){
        if(requestParams == null || requestParams.isEmpty()){
            throw invalidRequest("requestParams must not be empty");
        }
        X509Certificate x509Certificate = parseCertificate(verificationCertificate);
        if (x509Certificate == null) {
            throw invalidRequest("SP verification certificate is not configured");
        }
        String rawSamlRequest = requestParams.get("SAMLRequest");
        String rawSigAlg = requestParams.get("SigAlg");
        String rawSignature = requestParams.get("Signature");
        String rawRelayState = requestParams.get("RelayState");
        if (rawSamlRequest == null || rawSamlRequest.isBlank()) {
            throw invalidRequest("SAMLRequest must not be empty");
        }
        if (rawSigAlg == null || rawSigAlg.isBlank()) {
            throw invalidRequest("SigAlg must not be empty");
        }
        if (rawSignature == null || rawSignature.isBlank()) {
            throw invalidRequest("Signature must not be empty");
        }
        String decodedSigAlg = URLDecoder.decode(rawSigAlg, StandardCharsets.UTF_8);
        String decodedSignature = URLDecoder.decode(rawSignature, StandardCharsets.UTF_8);

        String jcaAlgorithm = REDIRECT_SIGNATURE_ALGORITHMS.get(decodedSigAlg);
        if (jcaAlgorithm == null) {
            throw invalidRequest("Unsupported signature algorithm");
        }
        try {
            StringBuilder signedQuery = new StringBuilder()
                    .append("SAMLRequest=")
                    .append(rawSamlRequest);
            if (rawRelayState != null) {
                signedQuery.append("&RelayState=").append(rawRelayState);
            }
            signedQuery.append("&SigAlg=").append(rawSigAlg);
            byte[] signedBytes = signedQuery.toString().getBytes(StandardCharsets.UTF_8);
            byte[] signatureBytes = Base64.getDecoder().decode(decodedSignature);
            java.security.Signature verifier = java.security.Signature.getInstance(jcaAlgorithm);
            verifier.initVerify(x509Certificate.getPublicKey());
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

    public void verifyPostSignature(AuthnRequest authnRequest, String verificationCertificate) {
        if (authnRequest == null) {
            throw invalidRequest("AuthnRequest must not be null");
        }
        if(verificationCertificate == null || verificationCertificate.isBlank()){
            throw invalidRequest("verificationCertificate must not be null");
        }
        X509Certificate certificate = parseCertificate(verificationCertificate);
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

    private String generateSamlId() {
        return "_" + SecureRandomUtils.generate();
    }

    public static Saml2IdpAuthenticationException invalidRequest(String description) {

        return new Saml2IdpAuthenticationException(new Saml2Error("urn:oasis:names:tc:SAML:2.0:status:Requester", description));
    }

    public static Saml2IdpAuthenticationException invalidResponse(String description) {

        return new Saml2IdpAuthenticationException(new Saml2Error("urn:oasis:names:tc:SAML:2.0:status:Response", description));
    }

}
