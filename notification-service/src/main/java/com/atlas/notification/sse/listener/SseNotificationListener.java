package com.atlas.notification.sse.listener;

import com.atlas.common.core.api.notification.body.CardBody;
import com.atlas.common.core.api.notification.builder.NotificationRequest;
import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.notification.service.NotificationService;
import com.atlas.notification.sse.event.SseConnectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/10 10:07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseNotificationListener {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    private final NotificationService notificationService;

    private final RedisHelper redisHelper;

    private static final String REDIS_KEY_PREFIX = "notification:github_star:sent:";

    @EventListener
    public void onSseConnected(SseConnectedEvent event) {
        Long userId = event.userId();
        log.debug("监听到 SSE 连接事件，准备执行上线后逻辑: userId={}", userId);

        String redisKey = REDIS_KEY_PREFIX + userId;
        if(redisHelper.setIfAbsent(redisKey,"1", Duration.ofDays(1))){
            scheduler.schedule(() -> {
                sendGitHubStarNotification(userId);
            }, 5, TimeUnit.SECONDS);
        }
    }

    private void sendGitHubStarNotification(Long userId) {
        notificationService.send(
                NotificationRequest.card("Atlas 项目动态", cardBodyBuilder -> cardBodyBuilder
                                .subTitle("Atlas 开源生态建设")
                                .content("Atlas 能够走到今天，离不开每一位开发者的支持。我们诚邀您访问 GitHub 仓库，共同见证项目的成长。如果您认可我们的努力，请点亮那一枚小小的 Star，这对我们至关重要！")
                                .tagText("官方动态")
                                .tagType(CardBody.TargetType.PROCESSING) // 使用蓝色 Info 代表官方正式发布
                                .link("https://github.com/yyyyyyyysssss/atlas")
                                .field(CardBody.KVField.builder()
                                        .label("仓库坐标")
                                        .value("yyyyyyyysssss/atlas")
                                        .highlight(true)
                                        .build())
                                .field(CardBody.KVField.builder()
                                        .label("当前目标")
                                        .value("收集 100 个 Star 达成 🚀")
                                        .build())
                                .action(CardBody.Action.builder()
                                        .label("立即前往 GitHub")
                                        .path("https://github.com/yyyyyyyysssss/atlas")
                                        .theme(CardBody.ActionTheme.PRIMARY)
                                        .actionType(CardBody.ActionType.URL)
                                        .target(CardBody.ActionTarget._BLANK)
                                        .build())
                        )
                        .inbox(NotificationEventEnum.NOTIFICATION_EVENT)
                        .noRecord()
                        .to()
                        .toUserIds(userId)
                        .build()
        );

    }

}
