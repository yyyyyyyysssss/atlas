package com.atlas.auth.service;

import com.atlas.auth.domain.vo.QrAuthStatusVO;
import com.atlas.auth.domain.vo.QrAuthTicketVO;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrAuthService {

    private final RedisHelper redisHelper;

    private final SecurityProperties securityProperties;

    /**
     * 二维码有效期（秒）
     */
    private static final long EXPIRE_SECONDS = 300;

    private static final String PENDING = "PENDING";
    private static final String SCANNED = "SCANNED";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String EXPIRED = "EXPIRED";

    private static final String QR_SCENE_KEY = "auth:qr:scene:";

    public QrAuthTicketVO ticket() {
        String sceneId = UUID.randomUUID().toString().replaceAll("-", "");

        String redisKey = QR_SCENE_KEY + sceneId;
        redisHelper.addHash(redisKey,"status",PENDING,Duration.ofSeconds(EXPIRE_SECONDS));

        log.info("生成二维码登录凭证: {}", sceneId);
        String qrUrl = securityProperties.getUiUrl() + "/qr/scan?sceneId=" + sceneId;
        return QrAuthTicketVO
                .builder()
                .sceneId(sceneId)
                .qrUrl(qrUrl)
                .expireSeconds(EXPIRE_SECONDS)
                .build();
    }

    public void confirm(String sceneId){

    }

    public QrAuthStatusVO status(String sceneId) {
        String redisKey = QR_SCENE_KEY + sceneId;
        Map<String, Object> map = redisHelper.getHashAll(redisKey);
        if (map == null || map.isEmpty()) {
            return QrAuthStatusVO
                    .builder()
                    .status(EXPIRED)
                    .build();
        }
        String status = (String) map.get("status");
        String accessToken = (String) map.get("token");
        log.debug("检查二维码状态 sceneId: {}, 状态: {}", sceneId, status);
        return QrAuthStatusVO
                .builder()
                .status(status)
                .token(accessToken)
                .build();
    }

}
