package com.atlas.auth.config.security.provider;

import com.atlas.auth.config.security.saml2.*;
import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.atlas.auth.service.Saml2RegisteredClientService;
import com.atlas.common.security.properties.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class Saml2IdpRequestAuthenticationProvider implements AuthenticationProvider {


    private final Saml2IdpService saml2IdpService;

    private final Saml2RegisteredClientService saml2RegisteredClientService;

    private final SecurityProperties securityProperties;

    private final Saml2IdpRequestContextRepository saml2IdpRequestContextRepository;

    private static final Set<String> SUPPORTED_RESPONSE_BINDINGS = Set.of(
            SAMLConstants.SAML2_POST_BINDING_URI,
            SAMLConstants.SAML2_REDIRECT_BINDING_URI
    );

    public Saml2IdpRequestAuthenticationProvider(Saml2IdpService saml2IdpService, Saml2RegisteredClientService saml2RegisteredClientService, SecurityProperties securityProperties, Saml2IdpRequestContextRepository saml2IdpRequestContextRepository) {
        this.saml2IdpService = saml2IdpService;
        this.saml2RegisteredClientService = saml2RegisteredClientService;
        this.securityProperties = securityProperties;
        this.saml2IdpRequestContextRepository = saml2IdpRequestContextRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Saml2IdpRequestAuthenticationToken requestAuthenticationToken  = (Saml2IdpRequestAuthenticationToken) authentication;
        String samlRequest = requestAuthenticationToken.getSamlRequest();
        Saml2MessageBinding binding = Saml2MessageBinding.valueOf(requestAuthenticationToken.getBinding());
        String rawQueryString = requestAuthenticationToken.getRawQueryString();
        if (samlRequest == null || samlRequest.isBlank()) {
            throw invalidRequest("invalid SAMLRequest");
        }
        log.info("SAML SSO request received, binding={}", binding.name());
        // 解析 AuthnRequest
        AuthnRequest authnRequest = this.saml2IdpService.authnRequest(samlRequest, binding);
        if (!SAMLVersion.VERSION_20.equals(authnRequest.getVersion())) {
            throw invalidRequest("Unsupported SAML protocol version", authnRequest);
        }
        // 校验响应方式
        String protocolBinding = authnRequest.getProtocolBinding();
        if (protocolBinding != null && !SUPPORTED_RESPONSE_BINDINGS.contains(protocolBinding)) {
            throw invalidRequest("Unsupported SAML Response binding", authnRequest);
        }
        // 校验 AuthnRequest 的时间，防止接受过期请求
        if (authnRequest.getIssueInstant() == null) {
            throw invalidRequest("AuthnRequest IssueInstant is required", authnRequest);
        }
        Instant issueInstant = authnRequest.getIssueInstant();
        Instant now = Instant.now();
        // 最多允许 1 分钟的未来时钟偏差
        if (issueInstant.isAfter(now.plusSeconds(60))) {
            throw invalidRequest("AuthnRequest IssueInstant is in the future", authnRequest);
        }
        // 请求最多允许存在 5 分钟
        if (issueInstant.isBefore(now.minusSeconds(300))) {
            throw invalidRequest("AuthnRequest IssueInstant has expired", authnRequest);
        }

        String requestId = authnRequest.getID();
        if (requestId == null || requestId.isBlank()) {
            throw invalidRequest("invalid AuthnRequest ID", authnRequest);
        }

        String issuer = authnRequest.getIssuer() != null ? authnRequest.getIssuer().getValue() : null;
        if (issuer == null || issuer.isBlank()) {
            throw invalidRequest("invalid AuthnRequest Issuer", authnRequest);
        }

        // 校验 Destination
        String destination = authnRequest.getDestination();
        String expectedDestination = securityProperties.getSaml2().getIdp().getSsoUrl();
        if (destination == null || destination.isBlank()) {
            throw invalidRequest("AuthnRequest Destination is required", authnRequest);
        }
        if (!expectedDestination.equals(destination)) {
            throw invalidRequest("Invalid AuthnRequest Destination", authnRequest);
        }

        // 当前仅支持通过已注册的 ACS URL 确定响应地址
        if (authnRequest.getAssertionConsumerServiceIndex() != null) {
            throw invalidRequest("AssertionConsumerServiceIndex is not supported", authnRequest);
        }
        // 根据 Issuer 查询已注册的 SP
        Saml2RegisteredClient saml2RegisteredClient = saml2RegisteredClientService.getEnabledByEntityId(issuer);
        if (saml2RegisteredClient == null) {
            throw invalidRequest("Unregistered SAML Service Provider", authnRequest);
        }
        // 校验 Entity ID
        if (saml2RegisteredClient.getEntityId() == null || !issuer.equals(saml2RegisteredClient.getEntityId())) {
            throw invalidRequest("SAML Service Provider Entity ID mismatch", authnRequest);
        }
        String acsUrl = authnRequest.getAssertionConsumerServiceURL();
        // 校验 ACS 配置
        String registeredAcsUrl = saml2RegisteredClient.getAcsUrl();
        if (registeredAcsUrl == null || registeredAcsUrl.isBlank()) {
            throw invalidRequest("SAML Service Provider ACS URL is not configured", authnRequest);
        }
        // 请求显式指定 ACS 时，必须与已注册地址完全一致
        if (!registeredAcsUrl.equals(acsUrl)) {
            throw invalidRequest("Unregistered AssertionConsumerServiceURL", authnRequest);
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
            params = new HashMap<>();
            if (rawQueryString == null || rawQueryString.isBlank()) {
                throw invalidRequest("Missing query string", authnRequest);
            }
            for (String pair : rawQueryString.split("&")) {
                int index = pair.indexOf('=');
                if (index <= 0) {
                    continue;
                }
                String key = pair.substring(0, index);
                String value = pair.substring(index + 1);
                if (params.putIfAbsent(key, value) != null) {
                    throw invalidRequest("Duplicate SAML query parameter", authnRequest);
                }
            }
            // Redirect 绑定使用查询参数签名
            boolean hasSigAlg = params.containsKey("SigAlg");
            boolean hasSignature = params.containsKey("Signature");
            // 只要出现任意一个签名参数，就视为尝试签名
            // 避免攻击者只传入部分签名参数而绕过检查
            if (hasSigAlg != hasSignature) {
                throw invalidRequest("Incomplete Redirect signature parameters", authnRequest);
            }
            signaturePresent = hasSignature;
        } else if (Saml2MessageBinding.POST.equals(binding)) {
            // POST 绑定通过 XML 内部的 ds:Signature 签名
            signaturePresent = authnRequest.getSignature() != null;
        } else {
            throw invalidRequest("Unsupported SAML binding", authnRequest);
        }
        // 要求签名时，如果请求没有携带签名，直接拒绝
        if (requireSignature && !signaturePresent) {
            throw invalidRequest("AuthnRequest signature is required", authnRequest);
        }
        // 只要实际携带签名，就必须验证 不受 requireSignedAuthnRequest 配置影响
        if (signaturePresent) {
            try {
                if (Saml2MessageBinding.REDIRECT.equals(binding)) {
                    this.saml2IdpService.verifyRedirectSignature(params, saml2RegisteredClient.getVerificationCertificate());
                } else {
                    this.saml2IdpService.verifyPostSignature(authnRequest, saml2RegisteredClient.getVerificationCertificate());
                }
            }catch (Saml2IdpAuthenticationException e){
                throw invalidRequest(e.getMessage(), authnRequest);
            }
            signatureVerified = true;
        }
        // 防重放
        if (!saml2IdpRequestContextRepository.markAsSeen(issuer, requestId)){
            throw invalidRequest("Duplicate AuthnRequest", authnRequest);
        }
        // 创建上下文
        Saml2IdpRequestContext context = Saml2IdpRequestContext
                .builder()
                .requestId(requestId)
                .spEntityId(issuer)
                .registeredClientId(saml2RegisteredClient.getId())
                .acsUrl(acsUrl)
                .relayState(requestAuthenticationToken.getRelayState())
                .responseBinding(authnRequest.getProtocolBinding())
                .nameIdFormat(authnRequest.getNameIDPolicy() != null ? authnRequest.getNameIDPolicy().getFormat() : null)
                .forceAuthn(Boolean.TRUE.equals(authnRequest.isForceAuthn()))
                .passive(Boolean.TRUE.equals(authnRequest.isPassive()))
                .createdAt(Instant.now())
                .build();


        // 是否强制重新认证 先不做处理 后续增加重新认证再处理
        boolean forceAuthn = Boolean.TRUE.equals(authnRequest.isForceAuthn());
        if (forceAuthn) {
            log.info("ForceAuthn requested but user already authenticated");
        }
        // 判断是否已认证
        Authentication currentAuthentication  = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = currentAuthentication != null
                && currentAuthentication.isAuthenticated()
                && !(currentAuthentication instanceof AnonymousAuthenticationToken);
        if (!authenticated) {
            // 是否禁止交互认证 只能使用已有登录态
            if(Boolean.TRUE.equals(authnRequest.isPassive())){
                throw invalidRequest("Passive authentication failed", authnRequest);
            }
            // 保存上下文
            saml2IdpRequestContextRepository.save(issuer, context);

            throw new Saml2IdpAuthenticationRequiredException(issuer, requestId);
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
        return new Saml2IdpAuthenticationToken(currentAuthentication.getPrincipal(), context, currentAuthentication.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {

        return Saml2IdpRequestAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public static Saml2IdpAuthenticationException invalidRequest(String description) {

        return invalidRequest(description, null);
    }

    public static Saml2IdpAuthenticationException invalidRequest(String description, AuthnRequest authnRequest) {

        return new Saml2IdpAuthenticationException(new Saml2Error(Saml2ErrorCodes.INVALID_REQUEST, description), authnRequest);
    }


}
