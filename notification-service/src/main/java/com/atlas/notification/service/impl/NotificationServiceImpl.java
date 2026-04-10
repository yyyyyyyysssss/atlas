package com.atlas.notification.service.impl;

import com.atlas.common.core.api.notification.builder.NotificationDTO;
import com.atlas.common.core.api.notification.enums.*;
import com.atlas.common.core.api.notification.exception.NotificationException;
import com.atlas.notification.adapter.MessageAdapter;
import com.atlas.notification.config.idwork.IdGen;
import com.atlas.notification.domain.entity.Notification;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
import com.atlas.notification.domain.vo.NotificationTemplateVO;
import com.atlas.notification.enums.NotificationErrorCode;
import com.atlas.notification.enums.NotificationStatus;
import com.atlas.notification.mapper.NotificationMapper;
import com.atlas.notification.service.AccountResolver;
import com.atlas.notification.service.NotificationTemplateService;
import com.atlas.notification.service.NotificationService;
import com.atlas.notification.service.render.RenderStrategy;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;

/**
 * (NtfNotification)表服务实现类
 *
 * @author ys
 * @since 2026-04-01 09:44:19
 */
@Service("notificationService")
@AllArgsConstructor
@Slf4j
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final List<MessageAdapter> messageAdapters;

    private final List<RenderStrategy> renderStrategies;

    private final NotificationTemplateService messageTemplateService;

    private final AccountResolver accountResolver;

    private final NotificationMapper notificationMapper;

    @Async
    @Override
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
                Long notificationId = IdGen.genId();
                Notification notification = Notification
                        .builder()
                        .title(ctx.getTitle())
                        .channelType(channel)
                        .templateCode(ctx.getTemplateCode())
                        .status(NotificationStatus.SENDING)
                        .params(ctx.getParams())
                        .ext(ctx.getExt())
                        .build();
                notification.setId(notificationId);

                try {
                    // 根据渠道 TargetType 解析账号
                    List<String> accounts = accountResolver.resolve(channel, ctx.getTargetType(), ctx.getTargets());
                    if (accounts.isEmpty()) {
                        throw new NotificationException(NotificationErrorCode.RECIPIENT_NOT_FOUND);
                    }

                    // 构建模型
                    MessageTemplateModel messageTemplateModel = resolveTemplateModel(ctx, channel);

                    // 匹配并执行渲染
                    RenderStrategy renderStrategy = renderStrategies.stream()
                            .filter(f -> f.support(messageTemplateModel.getContentType()))
                            .findFirst()
                            .orElseThrow(() -> new NotificationException(NotificationErrorCode.RENDER_STRATEGY_NOT_SUPPORT));

                    MessagePayload messagePayload = renderStrategy.render(messageTemplateModel, ctx.getParams(), ctx.getExt());

                    if (ctx.isRecord()) {
                        notification.setContent(messagePayload.getContent());
                        notification.setContentType(messagePayload.getContentType());
                        notification.setCategory(messagePayload.getCategory());
                        notification.setSendTime(messagePayload.getSendTime());
                        notificationMapper.insert(notification);
                    }


                    // 分发路由
                    MessageAdapter adapter = dispatch(channel);

                    // 发送
                    adapter.send(messagePayload, accounts);

                    if (ctx.isRecord()) {
                        this.lambdaUpdate()
                                .set(Notification::getStatus, NotificationStatus.SENT)
                                .eq(Notification::getId, notificationId)
                                .update();
                    }

                } catch (Exception e) {
                    // 记录具体某个渠道的失败，不影响其他渠道
                    log.error("[Notification-Engine] [{}] Send Failed {} Reason: {}", channel, logId, e.getMessage(), e);
                    if (ctx.isRecord()) {
                        ensureRecordFailed(notification, e.getMessage());
                    }
                }
            }
        } catch (NotificationException e) {
            log.error("[Notification-Engine] Send Failed {} ErrorCode: {} Reason: {}", logId, e.getCode(), e.getDetail());
        } catch (Exception e) {
            log.error("[Notification-Engine] Send Failed {} Reason: {}", logId, e.getMessage(), e);
            // todo 修改或保存数据库
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            log.info("[Notification-Engine] {} Cost: {}s", logId, String.format("%.3f", stopWatch.getTotalTimeSeconds()));
            if (log.isDebugEnabled()) {
                log.debug("\n{}", stopWatch.prettyPrint());
            }
        }

    }

    private void ensureRecordFailed(Notification notification, String failReason) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setFailReason(failReason);
        this.saveOrUpdate(notification);
    }

    private void updateStatus(Long notificationId, NotificationStatus status) {
        this.lambdaUpdate()
                .set(Notification::getStatus, status)
                .eq(Notification::getId, notificationId)
                .update();
    }

    private MessageAdapter dispatch(ChannelType channelType) {
        return messageAdapters
                .stream()
                .filter(a -> a.support(channelType))
                .findFirst()
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.CHANNEL_TYPE_NOT_SUPPORT, " ChannelType: " + channelType));
    }

    private MessageTemplateModel resolveTemplateModel(NotificationDTO ctx, ChannelType channelType) {
        MessageTemplateModel.MessageTemplateModelBuilder builder = MessageTemplateModel
                .builder()
                .templateCode(ctx.getTemplateCode())
                .title(ctx.getTitle())
                .category(ctx.getCategory())
                .content(ctx.getContent())
                .contentType(ctx.getContentType());
        RenderType finalRenderType = ctx.renderType();
        if (StringUtils.isNotEmpty(ctx.getTemplateCode())) {
            NotificationTemplateVO messageTemplateVO = messageTemplateService.resolveTemplate(ctx.getTemplateCode(), channelType);
            if (messageTemplateVO == null) {
                throw new NotificationException(NotificationErrorCode.TEMPLATE_NOT_FOUND, "TemplateCode: " + ctx.getTemplateCode());
            }
            // 如果 DSL 没指定 renderType，则使用模板里配置的
            if (finalRenderType == null) {
                finalRenderType = messageTemplateVO.getRenderType();
            }
            builder
                    .templateId(messageTemplateVO.getId())
                    .templateCode(messageTemplateVO.getCode())
                    // 数据库有标题用数据库的，没有用 Context 传入的（如代码直接指定的本地模板标题）
                    .title(StringUtils.defaultIfBlank(messageTemplateVO.getTitle(), ctx.getTitle()))
                    .category(messageTemplateVO.getCategory())
                    .content(messageTemplateVO.getContent())
                    .contentType(ContentType.getContentType(channelType))
                    .extTemplateCode(messageTemplateVO.getExtTemplateCode())
                    .build();
        }
        builder.renderType(finalRenderType);
        return builder.build();
    }

}

