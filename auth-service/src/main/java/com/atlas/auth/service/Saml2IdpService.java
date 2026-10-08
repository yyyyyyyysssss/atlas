package com.atlas.auth.service;

import com.atlas.common.security.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.IDPSSODescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.KeyDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.SingleSignOnServiceBuilder;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.impl.KeyInfoBuilder;
import org.opensaml.xmlsec.signature.impl.X509CertificateBuilder;
import org.opensaml.xmlsec.signature.impl.X509DataBuilder;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/8 14:38
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Saml2IdpService {

    private final SecurityProperties securityProperties;

    static {
        OpenSamlInitializationService.initialize();
    }

    // 生成 SAML 2.0 IdP Metadata。
    public String metadata() {
        SecurityProperties.IdpConfig idp = securityProperties.getSaml2().getIdp();
        X509Certificate certificate = parseCertificate(idp.getSigning().getCertificate());
        EntityDescriptor entityDescriptor = buildMetadata(idp, certificate);
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

    public String sso(String samlRequest, Saml2MessageBinding binding){
        if (samlRequest == null || samlRequest.isBlank()) {
            throw new IllegalArgumentException("Missing SAMLRequest");
        }
        if (binding == null) {
            throw new IllegalArgumentException("Missing SAML binding");
        }
        log.info("SAML SSO request received, binding={}", binding.name());
        log.info("SAMLRequest: {}", samlRequest);
        return null;
    }


    private EntityDescriptor buildMetadata(SecurityProperties.IdpConfig idp, X509Certificate certificate){
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
    private KeyInfo buildKeyInfo(X509Certificate certificate){
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

}
