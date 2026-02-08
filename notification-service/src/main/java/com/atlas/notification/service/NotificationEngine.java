package com.atlas.notification.service;

import com.atlas.common.api.UserApi;
import com.atlas.common.api.dto.NotificationDTO;
import com.atlas.common.api.dto.UserDTO;
import com.atlas.common.api.enums.ChannelType;
import com.atlas.common.api.enums.TargetType;
import com.atlas.common.api.exception.NotificationException;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
import com.atlas.notification.domain.vo.MessageTemplateVO;
import com.atlas.notification.enums.ActivationStatus;
import com.atlas.notification.enums.DisplayType;
import com.atlas.notification.enums.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 15:32
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEngine {

    private final List<MessageAdapter> messageAdapters;

    private final List<RenderStrategy> renderStrategies;

    private final MessageTemplateService messageTemplateService;

    private final UserApi userApi;

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
                    List<String> realAccounts = resolveRealAccounts(channel, ctx.getTargetType(), ctx.getTargets());
                    if (realAccounts.isEmpty()) {
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
                    adapter.send(messagePayload, realAccounts);

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

    private List<String> resolveRealAccounts(ChannelType channel, TargetType targetType, List<String> targets) {
        if (CollectionUtils.isEmpty(targets)) {
            return Collections.emptyList();
        }
        // 渠道需要的目标类型
        TargetType requiredType = TargetTypeResolver.getRequiredType(channel);
        // 匹配则直发
        if (targetType == requiredType) {
            return targets;
        }

        List<UserDTO> users = switch (targetType) {
            case USER_ID -> userApi.findByUserId(targets.stream().map(Long::valueOf).toList());
            case EMAIL   -> userApi.findByEmail(targets);
            case PHONE   -> userApi.findByPhone(targets);
        };
        Function<UserDTO, String> getter = TargetTypeResolver.getGetter(requiredType);
        return users.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private MessageTemplateModel resolveTemplateModel(NotificationDTO ctx, ChannelType channelType) {
        if (StringUtils.isNotEmpty(ctx.getTemplateCode())) {
            // 先尝试从数据库获取
            MessageTemplateVO messageTemplateVO = findMessageTemplate(ctx.getTemplateCode(), channelType);
            if (messageTemplateVO == null) {
                throw new NotificationException(NotificationErrorCode.TEMPLATE_NOT_FOUND, "TemplateCode: " + ctx.getTemplateCode());
            }
            if (ActivationStatus.INACTIVE.equals(messageTemplateVO.getStatus())) {
                throw new NotificationException(NotificationErrorCode.TEMPLATE_DISABLED, "TemplateCode: " + ctx.getTemplateCode());
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

    public MessageTemplateVO findMessageTemplate(String code, ChannelType channelType) {
        // 1. 先查数据库
        MessageTemplateVO vo = messageTemplateService.findByCodeAndChannel(code, channelType);
        if (vo != null) {
            return vo;
        }
        // 2. 数据库没有，尝试查找本地 Classpath
        return loadFromClasspath(code, channelType);
    }

    private MessageTemplateVO loadFromClasspath(String code, ChannelType channelType) {
        String suffix = (channelType == ChannelType.SMS) ? ".txt" : ".html";
        String fullPath = "templates/" + code;
        if (!fullPath.toLowerCase().endsWith(suffix)) {
            fullPath = fullPath + suffix;
        }
        Resource resource = new ClassPathResource(fullPath);
        if (!resource.exists()) {
            log.debug("Local Template not found: {}", fullPath);
            return null;
        }
        try (InputStream is = resource.getInputStream()) {
            String content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            MessageTemplateVO templateVO = new MessageTemplateVO();
            templateVO.setCode(code);
            templateVO.setContent(content);
            templateVO.setStatus(ActivationStatus.ACTIVE);
            if (channelType == ChannelType.SMS) {
                templateVO.setDisplayType(DisplayType.TEXT);
            } else {
                // 邮件、SSE 等默认使用 HTML
                templateVO.setDisplayType(DisplayType.HTML);
            }
            return templateVO;
        } catch (IOException e) {
            throw new NotificationException("Read Local Template Failed [" + fullPath + "]");
        }
    }
}
