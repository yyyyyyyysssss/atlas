package com.atlas.auth.service;

import com.atlas.auth.enums.SecurityScene;

import java.time.Duration;

public interface CaptchaVerificationService {

    void send(String target, SecurityScene scene);

    void send(String target, SecurityScene scene, Duration duration);

    boolean verify(String target, String inputCode, SecurityScene scene);

}
