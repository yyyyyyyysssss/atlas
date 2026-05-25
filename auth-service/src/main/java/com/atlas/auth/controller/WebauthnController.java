package com.atlas.auth.controller;


import com.atlas.auth.domain.dto.WebAuthnRegistrationOptionsResponse;
import com.atlas.auth.domain.dto.WebAuthnRegistrationRequest;
import com.atlas.auth.domain.dto.WebauthnAuthenticateOptionsResponse;
import com.atlas.auth.domain.dto.WebauthnRegistrationResponse;
import com.atlas.auth.service.WebauthnService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/webauthn")
@RestController
@RequiredArgsConstructor
@Slf4j
public class WebauthnController {

    private final WebauthnService webauthnService;

    @PostMapping("/register/options")
    public WebAuthnRegistrationOptionsResponse registrationOptions(HttpServletRequest request, HttpServletResponse response) {

        return webauthnService.createRegistrationOptions(request, response);
    }

    @PostMapping("/register")
    public WebauthnRegistrationResponse register(HttpServletRequest request, @RequestBody WebAuthnRegistrationRequest webAuthnRegistrationRequest) {

        return webauthnService.registerCredential(request,webAuthnRegistrationRequest.publicKey());
    }

    @PostMapping("/authenticate/options")
    public WebauthnAuthenticateOptionsResponse authenticateOptions(HttpServletRequest request, HttpServletResponse response) {

        return webauthnService.authenticateOptions(request, response);
    }

}
