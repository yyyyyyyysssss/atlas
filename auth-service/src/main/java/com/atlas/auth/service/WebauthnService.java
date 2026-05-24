package com.atlas.auth.service;

import com.atlas.auth.config.security.webauthn.RedisPublicKeyCredentialCreationOptionsRepository;
import com.atlas.auth.config.security.webauthn.RedisPublicKeyCredentialRequestOptionsRepository;
import com.atlas.common.core.exception.BusinessException;
import com.fasterxml.jackson.databind.Module;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ResolvableType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;
import org.springframework.security.web.webauthn.management.*;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebauthnService {

    private final Webauthn4JRelyingPartyOperations webauthn4JRelyingPartyOperations;

    private final PublicKeyCredentialCreationOptionsRepository publicKeyCredentialCreationOptionsRepository;

    private final PublicKeyCredentialRequestOptionsRepository publicKeyCredentialRequestOptionsRepository;

    private GenericHttpMessageConverter<Object> genericHttpMessageConverter = new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().modules(new Module[]{new WebauthnJackson2Module()}).build());

    public PublicKeyCredentialCreationOptions createRegistrationOptions(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        PublicKeyCredentialCreationOptions options =
                webauthn4JRelyingPartyOperations.createPublicKeyCredentialCreationOptions(
                        new ImmutablePublicKeyCredentialCreationOptionsRequest(authentication)
                );
        publicKeyCredentialCreationOptionsRepository.save(request, response, options);
        return options;
    }

    public CredentialRecord registerCredential(HttpServletRequest request, RelyingPartyPublicKey publicKey) {
        PublicKeyCredentialCreationOptions options = this.publicKeyCredentialCreationOptionsRepository.load(request);
        if (options == null) {
            throw new BusinessException("challenge expired");
        }
        try {
            return webauthn4JRelyingPartyOperations.registerCredential(
                    new ImmutableRelyingPartyRegistrationRequest(options, publicKey)
            );
        } finally {
            ((RedisPublicKeyCredentialCreationOptionsRepository) publicKeyCredentialCreationOptionsRepository).remove(request);
        }

    }

    public PublicKeyCredentialRequestOptions authenticateOptions(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        ImmutablePublicKeyCredentialRequestOptionsRequest optionsRequest = new ImmutablePublicKeyCredentialRequestOptionsRequest(authentication);
        PublicKeyCredentialRequestOptions credentialRequestOptions = webauthn4JRelyingPartyOperations.createCredentialRequestOptions(optionsRequest);
        publicKeyCredentialRequestOptionsRepository.save(request, response, credentialRequestOptions);
        return credentialRequestOptions;
    }

    public PublicKeyCredentialUserEntity authenticate(HttpServletRequest request){
        ServletServerHttpRequest httpRequest = new ServletServerHttpRequest(request);
        ResolvableType resolvableType = ResolvableType.forClassWithGenerics(PublicKeyCredential.class, new Class[]{AuthenticatorAssertionResponse.class});
        PublicKeyCredential<AuthenticatorAssertionResponse> publicKeyCredential = null;
        try {
            publicKeyCredential = (PublicKeyCredential)this.genericHttpMessageConverter.read(resolvableType.getType(), this.getClass(), httpRequest);
        } catch (Exception ex) {
            throw new BadCredentialsException("Unable to authenticate the PublicKeyCredential", ex);
        }
        PublicKeyCredentialRequestOptions requestOptions = publicKeyCredentialRequestOptionsRepository.load(request);
        if (requestOptions == null) {
            throw new BadCredentialsException("Unable to authenticate the PublicKeyCredential. No PublicKeyCredentialRequestOptions found.");
        }
        RelyingPartyAuthenticationRequest authenticationRequest = new RelyingPartyAuthenticationRequest(requestOptions, publicKeyCredential);
        // 删除
        ((RedisPublicKeyCredentialRequestOptionsRepository) publicKeyCredentialRequestOptionsRepository).remove(request);
        return webauthn4JRelyingPartyOperations.authenticate(authenticationRequest);
    }

}
