package com.atlas.auth.controller;

import com.atlas.auth.config.security.saml2.Saml2IdpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.web.bind.annotation.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/8 15:09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/saml2/idp")
public class Saml2IdpController {

    private final Saml2IdpService saml2IdpService;

    @GetMapping(value = "/metadata", produces = "application/samlmetadata+xml")
    public ResponseEntity<String> metadata() {

        return ResponseEntity.ok(saml2IdpService.metadata());
    }

    /**
     * SAML 2.0 SSO。
     * HTTP-Redirect Binding。
     * AuthnRequest 通常通过 URL 参数 SAMLRequest 传递。
     */
    @GetMapping("/sso")
    public ResponseEntity<String> ssoGet(@RequestParam("SAMLRequest") String samlRequest){
        String ssoResponse = saml2IdpService.sso(samlRequest, Saml2MessageBinding.REDIRECT);
        return ResponseEntity.ok(ssoResponse);
    }

    /**
     * SAML 2.0 SSO。
     * HTTP-POST Binding。
     * AuthnRequest 通常通过 form 参数 SAMLRequest 传递。
     */
    @PostMapping("/sso")
    public ResponseEntity<String> ssoPost(@RequestParam("SAMLRequest") String samlRequest) {
        String ssoResponse = saml2IdpService.sso(samlRequest, Saml2MessageBinding.POST);
        return ResponseEntity.ok(ssoResponse);
    }

}
