package com.atlas.notification.service.impl;

import com.atlas.common.core.api.notification.builder.NotificationDTO;
import com.atlas.common.core.api.notification.enums.*;
import com.atlas.common.core.api.notification.exception.NotificationException;
import com.atlas.notification.adapter.MessageAdapter;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.notification.domain.entity.Notification;
import com.atlas.notification.domain.entity.NotificationReceiver;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
import com.atlas.notification.domain.mode.ResolvedTarget;
import com.atlas.notification.domain.vo.NotificationTemplateVO;
import com.atlas.notification.domain.vo.UserNotificationVO;
import com.atlas.notification.enums.NotificationErrorCode;
import com.atlas.notification.enums.NotificationStatus;
import com.atlas.notification.mapper.NotificationMapper;
import com.atlas.notification.service.AccountResolver;
import com.atlas.notification.service.NotificationReceiverService;
import com.atlas.notification.service.NotificationTemplateService;
import com.atlas.notification.service.NotificationService;
import com.atlas.notification.service.render.RenderStrategy;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    private final NotificationReceiverService notificationReceiverService;

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
                    List<ResolvedTarget> accounts = accountResolver.resolve(channel, ctx.getTargetType(), ctx.getTargets());
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

                        saveNotificationReceivers(notification.getId(), ctx.getTargetType(), accounts);

                        // 设置消息id
                        messagePayload.setNotificationId(notification.getId());
                    }


                    // 分发路由
                    MessageAdapter adapter = dispatch(channel);

                    // 发送
                    adapter.send(messagePayload, accounts.stream().map(ResolvedTarget::getAccount).toList());

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


    private void saveNotificationReceivers(Long notificationId, TargetType targetType, List<ResolvedTarget> resolvedTargets) {
        if (CollectionUtils.isEmpty(resolvedTargets)) {
            log.warn("[Notification-Engine] No resolved targets to save for notificationId: {}", notificationId);
            return;
        }
        List<NotificationReceiver> receivers = resolvedTargets.stream().map(target -> {
            NotificationReceiver receiver = new NotificationReceiver();
            receiver.setId(IdGen.genId());
            receiver.setNotificationId(notificationId);
            receiver.setTargetType(targetType);
            receiver.setReceiverId(target.getUserId() != null ? target.getUserId() : null);
            receiver.setReceiverAccount(target.getAccount());
            receiver.setIsRead(false);
            receiver.setReceiveTime(LocalDateTime.now());
            return receiver;
        }).collect(Collectors.toList());
        // 执行批量插入
        try {
            notificationReceiverService.saveBatch(receivers);
            log.info("[Notification-Engine] Successfully saved {} receiver records for notificationId: {}",
                    receivers.size(), notificationId);
        } catch (Exception e) {
            // 记录异常，但不建议抛出，以免影响已经触发的发送动作
            log.error("[Notification-Engine] Failed to save receiver records for notificationId: {}. Reason: {}",
                    notificationId, e.getMessage(), e);
        }
    }

    @Override
    public Integer countUnread(Long userId) {
        Long count = notificationReceiverService
                .lambdaQuery()
                .eq(NotificationReceiver::getReceiverId, userId)
                .eq(NotificationReceiver::getIsRead, false)
                .count();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public PageInfo<UserNotificationVO> userNotificationList(Long userId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<UserNotificationVO> userNotificationVOS = notificationMapper.selectUserNotifications(userId);
        return PageInfo.of(userNotificationVOS);
    }


    @Override
    public void markAsRead(Long userId, Long notificationId) {
        notificationReceiverService
                .lambdaUpdate()
                .set(NotificationReceiver::getIsRead, true)
                .eq(NotificationReceiver::getReceiverId, userId)
                .eq(NotificationReceiver::getNotificationId, notificationId)
                .update();
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationReceiverService
                .lambdaUpdate()
                .set(NotificationReceiver::getIsRead, true)
                .eq(NotificationReceiver::getReceiverId, userId)
                .eq(NotificationReceiver::getIsRead, false)
                .update();
    }
}

