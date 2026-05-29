package com.atlas.auth.service;

import com.atlas.auth.enums.CaptchaScene;

import java.time.Duration;

public interface CaptchaVerificationService {

    void send(String target, CaptchaScene scene);

    void send(String target, CaptchaScene scene, Duration duration);

    boolean verify(String target, String inputCode, CaptchaScene scene);

}
