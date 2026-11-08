package com.atlas.auth.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<Void> checkCurrentUser() {

        return ResponseEntity.noContent().build();
    }

}
