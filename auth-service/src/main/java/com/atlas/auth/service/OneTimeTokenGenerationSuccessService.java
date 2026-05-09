package com.atlas.auth.service;

import com.atlas.common.core.api.notification.NotificationApi;
import com.atlas.common.core.api.notification.builder.NotificationRequest;
import com.atlas.security.properties.SecurityProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/17 16:23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OneTimeTokenGenerationSuccessService implements OneTimeTokenGenerationSuccessHandler {

    private final SecurityProperties securityProperties;

    private final NotificationApi notificationApi;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken oneTimeToken) {
        String magicLink = securityProperties.getUiUrl() + "/login?ottToken=" + oneTimeToken.getTokenValue();
        log.info("magic link: {}", magicLink);
        LocalDateTime now = LocalDateTime.now();
        Instant nowInstant = now.atZone(ZoneId.systemDefault()).toInstant();
        Instant expiresAt = oneTimeToken.getExpiresAt();
        long minutes = Duration.between(nowInstant, expiresAt).toMinutes();
        notificationApi.send(
                NotificationRequest
                        .template("ott_login", Map.of("magicLink", magicLink))
                        .email()
                        .withParam("username",oneTimeToken.getUsername())
                        .withParam("requestTime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .withParam("expireMinutes", minutes)
                        .to()
                        .toUsernames(oneTimeToken.getUsername())
                        .build()
        );
    }
}
