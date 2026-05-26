package com.atlas.auth.config.security.webauthn;

import com.atlas.security.properties.SecurityProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.web.webauthn.api.*;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebauthnCredentialOptionsContext implements Serializable {

    private String challenge;

    private String userId;

    private String userName;

    private String userDisplayName;

    private Long timeoutValue;

    private String attestation;

    private String residentKey;

    private String userVerification;

    private List<CredentialDescriptor> excludeCredentials;

    private List<CredentialDescriptor> allowCredentials;

    private Map<String, Object> extensions;


    public static WebauthnCredentialOptionsContext of(PublicKeyCredentialCreationOptions options){
        if (options == null) {
            return null;
        }
        List<PublicKeyCredentialDescriptor> excludeCredentials = options.getExcludeCredentials();
        List<WebauthnCredentialOptionsContext.CredentialDescriptor> excludes= null;

        if (excludeCredentials != null) {
            excludes = excludeCredentials.stream()
                    .map(desc -> WebauthnCredentialOptionsContext.CredentialDescriptor.builder()
                            .id(desc.getId().toBase64UrlString())
                            .type(desc.getType().getValue())
                            .transports(desc.getTransports() == null ? null :
                                    desc.getTransports().stream()
                                            .map(org.springframework.security.web.webauthn.api.AuthenticatorTransport::getValue)
                                            .collect(java.util.stream.Collectors.toSet()))
                            .build())
                    .toList();
        }

        Map<String, Object> extensions = null;
        if (options.getExtensions() != null && options.getExtensions().getInputs() != null) {
            extensions = options.getExtensions().getInputs().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            AuthenticationExtensionsClientInput::getExtensionId,
                            AuthenticationExtensionsClientInput::getInput,
                            (v1, v2) -> v1 // 冲突时保留前者
                    ));
        }

        return WebauthnCredentialOptionsContext.builder()
                .challenge(options.getChallenge().toBase64UrlString())
                .userId(options.getUser().getId().toBase64UrlString())
                .userName(options.getUser().getName())
                .userDisplayName(options.getUser().getDisplayName())
                .timeoutValue(options.getTimeout() != null ? options.getTimeout().toMillis() : 300000L)
                .attestation(options.getAttestation().getValue())
                .residentKey(
                        options.getAuthenticatorSelection()
                                .getResidentKey()
                                .getValue()
                )
                .userVerification(
                        options.getAuthenticatorSelection()
                                .getUserVerification()
                                .getValue()
                )
                .excludeCredentials(excludes)
                .extensions(extensions)
                .build();
    }

    public static WebauthnCredentialOptionsContext of(PublicKeyCredentialRequestOptions options){
        if (options == null) {
            return null;
        }
        List<PublicKeyCredentialDescriptor> allowCredentials = options.getAllowCredentials();
        List<CredentialDescriptor> allows = null;
        if (allowCredentials != null) {
            allows = allowCredentials.stream()
                    .map(desc -> CredentialDescriptor.builder()
                            .id(desc.getId().toBase64UrlString())
                            .type(desc.getType().getValue())
                            .transports(desc.getTransports() == null ? null :
                                    desc.getTransports().stream()
                                            .map(AuthenticatorTransport::getValue)
                                            .collect(java.util.stream.Collectors.toSet()))
                            .build())
                    .toList();
        }

        Map<String, Object> extensions = null;
        if (options.getExtensions() != null && options.getExtensions().getInputs() != null) {
            extensions = options.getExtensions().getInputs().stream()
                    .collect(Collectors.toMap(
                            AuthenticationExtensionsClientInput::getExtensionId,
                            AuthenticationExtensionsClientInput::getInput,
                            (v1, v2) -> v1 // 冲突时保留前者
                    ));
        }

        return WebauthnCredentialOptionsContext.builder()
                .challenge(options.getChallenge().toBase64UrlString())
                .timeoutValue(options.getTimeout() != null ? options.getTimeout().toMillis() : 300000L)
                .userVerification(options.getUserVerification() != null ? options.getUserVerification().getValue() : null)
                .allowCredentials(allows)
                .extensions(extensions)
                .build();
    }

    public PublicKeyCredentialCreationOptions toPublicKeyCredentialCreationOptions(SecurityProperties securityProperties){
        if (this.challenge == null) {
            throw new IllegalStateException("Challenge cannot be null when reconstructing RequestOptions");
        }

        PublicKeyCredentialRpEntity rp = PublicKeyCredentialRpEntity.builder()
                .id(securityProperties.getWebauthn().getRpId())
                .name(securityProperties.getWebauthn().getRpName())
                .build();

        return PublicKeyCredentialCreationOptions.builder()
                .rp(rp)
                .user(toUserEntity())
                .challenge(Bytes.fromBase64(challenge))
                .pubKeyCredParams(defaultPublicKeyCredentialParameters())
                .timeout(Duration.ofMillis(timeoutValue))
                .attestation(toAttestationConveyancePreference())
                .authenticatorSelection(toAuthenticatorSelectionCriteria())
                .excludeCredentials(this.toDescriptor(this.excludeCredentials))
                .extensions(this.toAuthenticationExtensionsClientInputs())
                .build();
    }

    public PublicKeyCredentialRequestOptions toPublicKeyCredentialRequestOptions(SecurityProperties securityProperties){
        if (this.challenge == null) {
            throw new IllegalStateException("Challenge cannot be null when reconstructing RequestOptions");
        }

        return PublicKeyCredentialRequestOptions.builder()
                .challenge(Bytes.fromBase64(this.challenge))
                .rpId(securityProperties.getWebauthn().getRpId())
                .userVerification(toUserVerificationRequirement(this.userVerification))
                .allowCredentials(this.toDescriptor(this.allowCredentials))
                .extensions(this.toAuthenticationExtensionsClientInputs())
                .build();
    }

    public PublicKeyCredentialUserEntity toUserEntity(){

        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(Bytes.fromBase64(userId))
                .name(userName)
                .displayName(userDisplayName)
                .build();
    }

    public List<PublicKeyCredentialParameters> defaultPublicKeyCredentialParameters(){

        return List.of(
                PublicKeyCredentialParameters.ES256,
                PublicKeyCredentialParameters.RS256
        );
    }

    public AuthenticatorSelectionCriteria toAuthenticatorSelectionCriteria(){

        return AuthenticatorSelectionCriteria
                .builder()
                .residentKey(toResidentKeyRequirement(residentKey))
                .userVerification(toUserVerificationRequirement(userVerification))
                .build();
    }

    private List<PublicKeyCredentialDescriptor> toDescriptor(List<CredentialDescriptor> credentials){
        if(credentials == null || credentials.isEmpty()){
            return Collections.emptyList();
        }
        return credentials.stream()
                .map(dto -> {
                    var descBuilder = PublicKeyCredentialDescriptor.builder()
                            .id(Bytes.fromBase64(dto.getId()))
                            // 依据传输的字符串安全还原对应的枚举对象
                            .type(PublicKeyCredentialType.valueOf(dto.getType()));
                    // 还原关键的 transports 集合
                    if (dto.getTransports() != null) {
                        Set<AuthenticatorTransport> transportEnums = dto.getTransports().stream()
                                .map(tStr -> AuthenticatorTransport.valueOf(tStr.toUpperCase()))
                                .collect(Collectors.toSet());
                        descBuilder.transports(transportEnums);
                    }

                    return descBuilder.build();
                })
                .toList();
    }

    public AuthenticationExtensionsClientInputs toAuthenticationExtensionsClientInputs(){
        if (this.extensions == null || this.extensions.isEmpty()) {
            return null;
        }
        List<AuthenticationExtensionsClientInput> inputList = this.extensions.entrySet().stream()
                .map(entry ->
                        (AuthenticationExtensionsClientInput)new ImmutableAuthenticationExtensionsClientInput(entry.getKey(), entry.getValue())
                )
                .toList();
        return new ImmutableAuthenticationExtensionsClientInputs(inputList);
    }

    private ResidentKeyRequirement toResidentKeyRequirement(String value) {
        if (value == null) return ResidentKeyRequirement.PREFERRED;
        return switch (value.toLowerCase()) {
            case "required" -> ResidentKeyRequirement.REQUIRED;
            case "discouraged" -> ResidentKeyRequirement.DISCOURAGED;
            default -> ResidentKeyRequirement.PREFERRED;
        };
    }

    private UserVerificationRequirement toUserVerificationRequirement(String value) {
        if (value == null) return UserVerificationRequirement.PREFERRED;
        return switch (value.toLowerCase()) {
            case "required" -> UserVerificationRequirement.REQUIRED;
            case "discouraged" -> UserVerificationRequirement.DISCOURAGED;
            default -> UserVerificationRequirement.PREFERRED;
        };
    }

    public AttestationConveyancePreference toAttestationConveyancePreference(){
        if (attestation == null) {
            return AttestationConveyancePreference.NONE;
        }
        return switch (attestation.toLowerCase()) {
            case "direct" -> AttestationConveyancePreference.DIRECT;
            case "indirect" -> AttestationConveyancePreference.INDIRECT;
            case "enterprise" -> AttestationConveyancePreference.ENTERPRISE;
            default -> AttestationConveyancePreference.NONE;
        };
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CredentialDescriptor implements Serializable {

        private String id;               // 凭证 ID (Base64Url 字符串)
        private String type;             // 凭证类型 (通常是 PUBLIC_KEY)
        private Set<String> transports;  // 传输通道集合 (如 ["internal", "usb"])
    }

}
