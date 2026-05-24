package com.atlas.auth.controller;


import com.atlas.auth.service.WebauthnService;
import com.fasterxml.jackson.databind.Module;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequestMapping("/temp/webauthn")
@RestController
@RequiredArgsConstructor
@Slf4j
public class WebauthnController {

    private final WebauthnService webauthnService;

    private HttpMessageConverter<Object> converter = new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().modules(new Module[]{new WebauthnJackson2Module()}).build());

    @PostMapping("/register/options")
    public void registrationOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PublicKeyCredentialCreationOptions registrationOptions = webauthnService.createRegistrationOptions(request, response);
        response.setStatus(200);
        response.setHeader("Content-Type", "application/json");
        this.converter.write(registrationOptions, MediaType.APPLICATION_JSON, new ServletServerHttpResponse(response));
    }

    @PostMapping("/register")
    public void register(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpInputMessage inputMessage = new ServletServerHttpRequest(request);
        WebAuthnRegistrationRequest webAuthnRegistrationRequest = (WebAuthnRegistrationRequest)this.converter.read(WebAuthnRegistrationRequest.class, inputMessage);
        RelyingPartyPublicKey publicKey = webAuthnRegistrationRequest.getPublicKey();
        CredentialRecord credentialRecord = webauthnService.registerCredential(request, publicKey);
        SuccessfulUserRegistrationResponse registrationResponse = new SuccessfulUserRegistrationResponse(credentialRecord);
        ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
        this.converter.write(registrationResponse, MediaType.APPLICATION_JSON, outputMessage);
    }

    @PostMapping("/authenticate/options")
    public void authenticateOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PublicKeyCredentialRequestOptions requestOptions = webauthnService.authenticateOptions(request, response);
        response.setHeader("Content-Type", "application/json");
        this.converter.write(requestOptions, MediaType.APPLICATION_JSON, new ServletServerHttpResponse(response));
    }

    public static class SuccessfulUserRegistrationResponse {
        private final CredentialRecord credentialRecord;

        SuccessfulUserRegistrationResponse(CredentialRecord credentialRecord) {
            this.credentialRecord = credentialRecord;
        }

        public boolean isSuccess() {
            return true;
        }
    }

    static class WebAuthnRegistrationRequest {
        private RelyingPartyPublicKey publicKey;

        WebAuthnRegistrationRequest() {
        }

        RelyingPartyPublicKey getPublicKey() {
            return this.publicKey;
        }

        void setPublicKey(RelyingPartyPublicKey publicKey) {
            this.publicKey = publicKey;
        }
    }

}
