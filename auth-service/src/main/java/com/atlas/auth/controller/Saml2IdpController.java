package com.atlas.auth.controller;

import com.atlas.auth.config.security.saml2.Saml2IdpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
