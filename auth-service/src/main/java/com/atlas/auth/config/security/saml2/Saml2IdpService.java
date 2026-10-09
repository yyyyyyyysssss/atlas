package com.atlas.auth.config.security.saml2;

import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.atlas.common.crypto.random.SecureRandomUtils;
import com.atlas.common.security.model.SecurityUser;
import com.atlas.common.security.properties.SecurityProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
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
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.impl.KeyInfoBuilder;
import org.opensaml.xmlsec.signature.impl.X509CertificateBuilder;
import org.opensaml.xmlsec.signature.impl.X509DataBuilder;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/8 14:38
 */
@Component
@Slf4j
public class Saml2IdpService {

    private final SecurityProperties securityProperties;

    /**
     * IdP 签名证书。
     */
    @Getter
    private final X509Certificate signingCertificate;

    /**
     * OpenSAML 签名凭据。
     */
    @Getter
    private final BasicX509Credential signingCredential;

    private static final long RESPONSE_VALIDITY_SECONDS = 300;

    public Saml2IdpService(SecurityProperties securityProperties, ResourceLoader resourceLoader){
        this.securityProperties = securityProperties;
        SecurityProperties.SigningConfig config = securityProperties.getSaml2().getIdp().getSigning();
        String keyStoreLocation = config.getKeyStore();
        String keyStorePassword = config.getKeyStorePassword();
        String keyAlias = config.getKeyAlias();
        if (keyStoreLocation == null || keyStoreLocation.isBlank()) {
            throw new IllegalStateException("未配置 SAML IdP 签名密钥库路径");
        }

        if (keyStorePassword == null || keyStorePassword.isBlank()) {
            throw new IllegalStateException("未配置 SAML IdP 签名密钥库密码");
        }

        if (keyAlias == null || keyAlias.isBlank()) {
            throw new IllegalStateException("未配置 SAML IdP 签名密钥别名");
        }
        try {
            Resource resource = resourceLoader.getResource(keyStoreLocation);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            char[] password = keyStorePassword.toCharArray();

            try (InputStream input = resource.getInputStream()) {
                keyStore.load(input, password);
            }
            if (!keyStore.containsAlias(keyAlias)) {
                throw new IllegalStateException("SAML IdP 签名密钥库中不存在别名: " + keyAlias);
            }

            Key key = keyStore.getKey(keyAlias, password);

            if (!(key instanceof PrivateKey privateKey)) {
                throw new IllegalStateException("SAML IdP 指定条目不包含私钥: " + keyAlias);
            }

            Certificate certificate = keyStore.getCertificate(keyAlias);

            if (!(certificate instanceof X509Certificate x509Certificate)) {
                throw new IllegalStateException("SAML IdP 指定条目不包含 X.509 证书: " + keyAlias);
            }

            x509Certificate.checkValidity();
            this.signingCertificate = x509Certificate;
            this.signingCredential = new BasicX509Credential(x509Certificate, privateKey);
            log.info("[SAML2-IdP] 签名密钥加载成功，alias={}, algorithm={}", keyAlias, privateKey.getAlgorithm());
        }catch (Exception e){
            throw new IllegalStateException("加载 SAML IdP 签名密钥失败", e);
        }
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

    public String sso(String samlRequest, Saml2MessageBinding binding) {
        if (samlRequest == null || samlRequest.isBlank()) {
            invalidRequest("invalid SAMLRequest");
        }
        if (binding == null) {
            invalidRequest("invalid SAML binding");
        }
        log.info("SAML SSO request received, binding={}", binding.name());
        log.info("SAMLRequest: {}", samlRequest);
        AuthnRequest authnRequest = parseAuthnRequest(samlRequest, binding);
        String requestId = authnRequest.getID();
        String issuer = authnRequest.getIssuer() != null ? authnRequest.getIssuer().getValue() : null;

        if (requestId == null || requestId.isBlank()) {
            invalidRequest("invalid AuthnRequest ID");
        }

        if (issuer == null || issuer.isBlank()) {
            invalidRequest("invalid AuthnRequest Issuer");
        }


        String destination = authnRequest.getDestination();
        String assertionConsumerServiceUrl = authnRequest.getAssertionConsumerServiceURL();
        log.info(
                "[SAML2 IdP] AuthnRequest 解析完成，requestId={}, issuer={}, destination={}, acs={}",
                requestId,
                issuer,
                destination,
                assertionConsumerServiceUrl
        );
        return null;
    }

    private AuthnRequest parseAuthnRequest(String samlRequest, Saml2MessageBinding binding){
        try {
            // 第一步：Base64 解码
            byte[] decoded = Base64.getDecoder().decode(samlRequest);
            byte[] xmlBytes;
            if (Saml2MessageBinding.REDIRECT.equals(binding)) {
                // HTTP-Redirect 使用原始 DEFLATE（无 zlib header）
                try (InputStream input = new InflaterInputStream(new ByteArrayInputStream(decoded), new Inflater(true)); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    input.transferTo(output);
                    xmlBytes = output.toByteArray();
                }
            } else if (Saml2MessageBinding.POST.equals(binding)) {
                // HTTP-POST 不进行 DEFLATE 解压
                xmlBytes = decoded;
            } else {
                invalidRequest("Unsupported SAML binding: " + binding);
            }
            // 第二步：解析 XML
            BasicParserPool parserPool = (BasicParserPool) XMLObjectProviderRegistrySupport.getParserPool();
            if(parserPool == null){
                throw new IllegalStateException("OpenSAML XML ParserPool has not been initialized");
            }
            org.w3c.dom.Element element;
            try (InputStream input = new ByteArrayInputStream(xmlBytes)) {
                element = parserPool.parse(input).getDocumentElement();
            }
            // 第三步：通过 OpenSAML Unmarshaller 转为对象
            UnmarshallerFactory factory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();

            Unmarshaller unmarshaller = factory.getUnmarshaller(element);
            if (unmarshaller == null) {
                invalidRequest("Unsupported SAML XML element");
            }
            XMLObject xmlObject = unmarshaller.unmarshall(element);

            if (!(xmlObject instanceof AuthnRequest authnRequest)) {
                throw new IllegalArgumentException("SAMLRequest is not an AuthnRequest");
            }
            return authnRequest;

        } catch (Saml2AuthenticationException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            invalidRequest("Failed to parse SAML AuthnRequest");
        }
    }

    public Response createResponse(AuthnRequest authnRequest, Saml2RegisteredClient registeredClient, Authentication authentication) {
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
                invalidResponse("SAML Response DOM is null after marshalling");
            }
            return SerializeSupport.nodeToString(node);
        } catch (MarshallingException e) {
            throw new IllegalStateException("Failed to serialize SAML 2.0 Response", e);
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
            invalidResponse("Unsupported SAML object: " + object.getClass().getName());
        }
        try {
            Marshaller marshaller = XMLObjectProviderRegistrySupport
                            .getMarshallerFactory()
                            .getMarshaller(object);

            if (marshaller == null) {
                invalidResponse("No marshaller registered for SAML object");
            }
            marshaller.marshall(object);
            Signer.signObject(signature);
        } catch (MarshallingException | org.opensaml.xmlsec.signature.support.SignatureException e) {
            invalidResponse("Failed to sign SAML object: " + e.getMessage());
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
            invalidResponse("No OpenSAML builder registered for " + name);
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
        idpDescriptor.setWantAuthnRequestsSigned(false);
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
            throw new IllegalStateException("Invalid SAML IdP X.509 certificate", e);
        }
    }

    public static void invalidRequest(String description) {

        throw new Saml2AuthenticationException(new Saml2Error("urn:oasis:names:tc:SAML:2.0:status:Requester", description));
    }

    public static void invalidResponse(String description) {

        throw new Saml2AuthenticationException(new Saml2Error("urn:oasis:names:tc:SAML:2.0:status:Response", description));
    }

}
