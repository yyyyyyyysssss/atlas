package com.atlas.notification.service;

import com.atlas.common.api.dto.NotificationDTO;
import com.atlas.common.api.enums.ChannelType;
import com.atlas.common.api.exception.NotificationException;
import com.atlas.notification.adapter.MessageAdapter;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
import com.atlas.notification.domain.vo.MessageTemplateVO;
import com.atlas.notification.enums.DisplayType;
import com.atlas.notification.enums.NotificationErrorCode;
import com.atlas.notification.service.render.RenderStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 15:32
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final List<MessageAdapter> messageAdapters;

    private final List<RenderStrategy> renderStrategies;

    private final MessageTemplateService messageTemplateService;

    private final AccountResolver accountResolver;

    @Async("notificationExecutor")
    public void send(NotificationDTO ctx) {
        if (ctx == null) {
            log.error("[Notification-Engine] MessageContext is null");
            return;
        }
        StopWatch stopWatch = new StopWatch("Notification-Engine");
        stopWatch.start();
        // 识别标识
        String logId = "BusinessKey: " + StringUtils.defaultIfEmpty(ctx.getTemplateCode(), ctx.getTitle() + " ");

        try {
            // 基础校验
            log.info("[Notification-Engine] {} Channels={} Targets={}", logId, ctx.getChannels(), ctx.getTargets());

            // 分发给匹配的适配器发送
            for (ChannelType channel : ctx.getChannels()) {
                try {
                    // 根据渠道 TargetType 解析账号
                    List<String> accounts = accountResolver.resolve(channel, ctx.getTargetType(), ctx.getTargets());
                    if (accounts.isEmpty()) {
                        log.warn("[Notification-Engine] [{}] {} TargetType: {} OriginalTargets: {} Reason: No valid contact information found.", channel, log, ctx.getTargetType(), ctx.getTargets());
                        continue;
                    }

                    // 构建模型
                    MessageTemplateModel messageTemplateModel = resolveTemplateModel(ctx, channel);

                    // 匹配并执行渲染
                    RenderStrategy renderStrategy = renderStrategies.stream()
                            .filter(f -> f.support(messageTemplateModel.getDisplayType()))
                            .findFirst()
                            .orElseThrow(() -> new NotificationException(NotificationErrorCode.RENDER_STRATEGY_NOT_SUPPORT, " DisplayType: " + messageTemplateModel.getDisplayType()));
                    MessagePayload messagePayload = renderStrategy.render(messageTemplateModel, ctx.getParams(), ctx.getExt());

                    // 分发路由
                    MessageAdapter adapter = dispatch(channel);

                    // 发送
                    adapter.send(messagePayload, accounts);

                } catch (NotificationException e) {
                    log.error("[Notification-Engine] [{}] Send Failed {} ErrorCode: {} Reason: {}", channel, logId, e.getCode(), e.getDetail());
                } catch (Exception e) {
                    // 记录具体某个渠道的失败，不影响其他渠道
                    log.error("[Notification-Engine] [{}] Send Failed {} Reason: {}", channel, logId, e.getMessage(),e);
                    // todo 修改或保存数据库
                }

            }
        } catch (NotificationException e) {
            log.error("[Notification-Engine] Send Failed {} ErrorCode: {} Reason: {}", logId, e.getCode(), e.getDetail());
        } catch (Exception e) {
            log.error("[Notification-Engine] Send Failed {} Reason: {}", logId, e.getMessage(),e);
            // todo 修改或保存数据库
        } finally {
            if(stopWatch.isRunning()){
                stopWatch.stop();
            }
            log.info("[Notification-Engine] {} Cost: {}s", logId, String.format("%.3f", stopWatch.getTotalTimeSeconds()));
            if (log.isDebugEnabled()) {
                log.debug("\n{}", stopWatch.prettyPrint());
            }
        }

    }

    private MessageAdapter dispatch(ChannelType channelType) {
        return messageAdapters
                .stream()
                .filter(a -> a.support(channelType))
                .findFirst()
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.CHANNEL_TYPE_NOT_SUPPORT, " ChannelType: " + channelType));
    }

    private MessageTemplateModel resolveTemplateModel(NotificationDTO ctx, ChannelType channelType) {
        if (StringUtils.isNotEmpty(ctx.getTemplateCode())) {
            MessageTemplateVO messageTemplateVO = messageTemplateService.resolveTemplate(ctx.getTemplateCode(),channelType);
            if (messageTemplateVO == null) {
                throw new NotificationException(NotificationErrorCode.TEMPLATE_NOT_FOUND, "TemplateCode: " + ctx.getTemplateCode());
            }
            return MessageTemplateModel.builder()
                    .templateId(messageTemplateVO.getId())
                    .templateCode(messageTemplateVO.getCode())
                    // 数据库有标题用数据库的，没有用 Context 传入的（如代码直接指定的本地模板标题）
                    .title(StringUtils.defaultIfBlank(messageTemplateVO.getTitle(), ctx.getTitle()))
                    .content(messageTemplateVO.getContent())
                    .displayType(messageTemplateVO.getDisplayType())
                    .extTemplateCode(messageTemplateVO.getExtTemplateCode())
                    .build();
        } else {
            return MessageTemplateModel.builder()
                    .title(ctx.getTitle())
                    .content(ctx.getText())
                    .displayType(DisplayType.TEXT)
                    .build();
        }
    }
}
