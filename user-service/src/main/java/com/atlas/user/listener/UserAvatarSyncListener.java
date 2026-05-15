package com.atlas.user.listener;

import com.atlas.common.core.api.file.FileApi;
import com.atlas.common.core.response.Result;
import com.atlas.user.domain.entity.User;
import com.atlas.user.event.UserAvatarSyncEvent;
import com.atlas.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/15 16:35
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserAvatarSyncListener {

    private final UserService userService;

    private final FileApi fileApi;

    private final RestClient proxyRestClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAvatarSync(UserAvatarSyncEvent event) {
        String avatarUrl = event.avatarUrl();
        Long userId = event.userId();
        String fileName = "avatar";
        if (StringUtils.hasText(avatarUrl)) {
            int lastSlashIndex = avatarUrl.lastIndexOf("/");
            if (lastSlashIndex != -1) {
                fileName = avatarUrl.substring(lastSlashIndex + 1);
            }
        }

        // 处理 Query 参数
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }

        // 强制增加后缀并加上 userId 前缀保证唯一性
        final String finalFileName = userId + "_" + (fileName.contains(".") ? fileName : fileName + ".png");
        try {
            log.info("开始同步用户[{}]头像: {}", userId, avatarUrl);
            byte[] imageBytes = proxyRestClient
                    .get()
                    .uri(avatarUrl)
                    .retrieve()
                    .body(byte[].class);
            if (imageBytes.length == 0) {
                log.warn("未能从第三方 URL 下载到有效数据: {}", event.avatarUrl());
                return;
            }
            Resource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return finalFileName; // 必须有后缀，方便服务端识别 MIME 类型
                }
            };
            Result<String> result = fileApi.uploadSimple(resource);
            if(!result.isSucceed()){
                log.warn("头像转存失败: {}, error: {}", avatarUrl, result.getMessage());
                return;
            }
            String newAvatarUrl = result.getData();
            boolean updated = userService
                    .lambdaUpdate()
                    .set(User::getAvatar,newAvatarUrl)
                    .eq(User::getId,userId)
                    .update();
            if (updated) {
                log.info("用户[{}]头像同步成功: {}", userId, newAvatarUrl);
            }
        }catch (Exception e){
            log.error("同步第三方头像至本地异常, userId: {}, url: {}", userId, avatarUrl, e);
        }
    }

}
