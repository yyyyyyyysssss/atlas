package com.atlas.auth.service;

import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.common.core.api.notification.NotificationApi;
import com.atlas.common.core.api.notification.builder.NotificationRequest;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.properties.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private final UserIdentifierService userIdentifierService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken oneTimeToken) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        UserIdentifier userIdentifier = userIdentifierService.findByValue(oneTimeToken.getUsername());
        if(userIdentifier == null){
            // 即使用户不存在，也返回成功 攻击者从 HTTP 响应上看不出区别
            log.warn("用户尝试登录但不存在: {}", oneTimeToken.getUsername());
            response.getWriter().write(JsonUtils.toJson(ResultGenerator.ok()));
            return;
        }

        String queryString = request.getQueryString();
        String magicLink = securityProperties.getUiUrl() + "/login?ottToken=" + oneTimeToken.getTokenValue();
        if (queryString != null && !queryString.isEmpty()) {
            magicLink += "&" + queryString;
        }
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
                        .toUserIds(userIdentifier.getUserId())
                        .build()
        );

        response.getWriter().write(JsonUtils.toJson(ResultGenerator.ok()));
    }
}
