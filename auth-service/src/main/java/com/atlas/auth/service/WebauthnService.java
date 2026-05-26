package com.atlas.auth.service;

import com.atlas.auth.config.security.webauthn.RedisPublicKeyCredentialCreationOptionsRepository;
import com.atlas.auth.config.security.webauthn.RedisPublicKeyCredentialRequestOptionsRepository;
import com.atlas.auth.domain.dto.*;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.ServletHolder;
import com.atlas.security.token.WebauthnAuthenticationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.management.*;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebauthnService {

    private final Webauthn4JRelyingPartyOperations webauthn4JRelyingPartyOperations;

    private final PublicKeyCredentialCreationOptionsRepository publicKeyCredentialCreationOptionsRepository;

    private final PublicKeyCredentialRequestOptionsRepository publicKeyCredentialRequestOptionsRepository;

    public WebAuthnRegistrationOptionsResponse createRegistrationOptions(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        PublicKeyCredentialCreationOptions options = webauthn4JRelyingPartyOperations.createPublicKeyCredentialCreationOptions(
                new ImmutablePublicKeyCredentialCreationOptionsRequest(authentication)
        );
        String webauthnId = UUID.randomUUID().toString().replace("-", "");
        request.setAttribute("webauthnId",webauthnId);
        publicKeyCredentialCreationOptionsRepository.save(request, response, options);
        return new WebAuthnRegistrationOptionsResponse(webauthnId,WebAuthnRegistrationOptions.of(options));
    }

    public WebauthnRegistrationResponse registerCredential(HttpServletRequest request, WebauthnRelyingPartyPublicKey webauthnRelyingPartyPublicKey) {
        RelyingPartyPublicKey publicKey = webauthnRelyingPartyPublicKey.toRelyingPartyPublicKey();
        PublicKeyCredentialCreationOptions options = this.publicKeyCredentialCreationOptionsRepository.load(request);
        if (options == null) {
            throw new BusinessException("challenge expired");
        }
        try {
            CredentialRecord credentialRecord = webauthn4JRelyingPartyOperations.registerCredential(
                    new ImmutableRelyingPartyRegistrationRequest(options, publicKey)
            );
            String credentialId = credentialRecord.getCredentialId().toBase64UrlString();
            return new WebauthnRegistrationResponse(credentialId,true);
        } finally {
            ((RedisPublicKeyCredentialCreationOptionsRepository) publicKeyCredentialCreationOptionsRepository).remove(request);
        }
    }

    public WebauthnAuthenticateOptionsResponse authenticateOptions(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        ImmutablePublicKeyCredentialRequestOptionsRequest optionsRequest = new ImmutablePublicKeyCredentialRequestOptionsRequest(authentication);
        PublicKeyCredentialRequestOptions credentialRequestOptions = webauthn4JRelyingPartyOperations.createCredentialRequestOptions(optionsRequest);
        String webauthnId = UUID.randomUUID().toString().replace("-", "");
        request.setAttribute("webauthnId",webauthnId);
        publicKeyCredentialRequestOptionsRepository.save(request, response, credentialRequestOptions);
        return new WebauthnAuthenticateOptionsResponse(webauthnId,WebauthnAuthenticateOptions.of(credentialRequestOptions));
    }

    public WebauthnAuthenticateResponse authenticate(WebauthnAuthenticationRequest webauthnAuthenticationRequest) {
        // 转换 DTO
        PublicKeyCredential<AuthenticatorAssertionResponse> publicKeyCredential = WebauthnAuthenticationRequest.toCredential(webauthnAuthenticationRequest);
        // 加载挑战字 options
        HttpServletRequest request = ServletHolder.getRequest();
        PublicKeyCredentialRequestOptions requestOptions = publicKeyCredentialRequestOptionsRepository.load(request);
        if (requestOptions == null) {
            throw new BadCredentialsException("Unable to authenticate the PublicKeyCredential. No PublicKeyCredentialRequestOptions found.");
        }
        // 组装并执行认证
        RelyingPartyAuthenticationRequest authenticationRequest = new RelyingPartyAuthenticationRequest(requestOptions, publicKeyCredential);
        PublicKeyCredentialUserEntity authenticate = webauthn4JRelyingPartyOperations.authenticate(authenticationRequest);

        // 认证成功后再安全移除
        if (publicKeyCredentialRequestOptionsRepository instanceof RedisPublicKeyCredentialRequestOptionsRepository redisRepo) {
            redisRepo.remove(request);
        }
        Long userId = Long.parseLong(new String(authenticate.getId().getBytes(), StandardCharsets.UTF_8));
        return new WebauthnAuthenticateResponse(
                webauthnAuthenticationRequest.id(),
                userId,
                true
        );
    }

}
