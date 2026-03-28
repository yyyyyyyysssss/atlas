package com.atlas.common.core.api.notification.builder;

import com.atlas.common.core.api.notification.constant.NotificationConstant;
import com.atlas.common.core.api.notification.dto.NotificationDTO;
import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import com.atlas.common.core.api.notification.enums.TargetType;
import com.atlas.common.core.api.notification.exception.NotificationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 9:52
 */
@Slf4j
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 只能通过静态方法或本类方法构建
public class NotificationRequest {

    // 模板编码
    private String templateCode;

    // 可选标题
    private String title;

    // 直接发送的原始文本
    private String text;

    private Object contentData;

    // 接收目标
    private List<String> targets;

    private TargetType targetType;

    // 发送渠道
    private List<ChannelType> channels;

    // 占位符变量
    private Map<String, Object> params;

    // 扩展参数
    private Map<String, Object> ext;

    /**
     * 以纯文本方式起手 (可选)
     */
    public static ChannelOp text(String title, String content) {
        NotificationRequest ctx = new NotificationRequest();
        ctx.title = title;
        ctx.text = content;
        return new NotificationBuilder(ctx);
    }

    public static ChannelOp text(String content) {

        return text(null, content);
    }


    /**
     * 以模板方式起手
     */
    public static ChannelOp template(String code, Map<String, Object> params) {

        return template(code, null, params);
    }

    public static ChannelOp template(String code, String title, Map<String, Object> params) {
        NotificationRequest ctx = new NotificationRequest();
        ctx.templateCode = code;
        ctx.title = title;
        ctx.params = params != null ? new HashMap<>(params) : new HashMap<>();
        return new NotificationBuilder(ctx);
    }

    /**
     * 以纯数据对象起手 (主要针对 SSE/WebSocket 场景)
     */
    public static ChannelOp data(Object data) {
        NotificationRequest ctx = new NotificationRequest();
        ctx.contentData = data;
        return new NotificationBuilder(ctx);
    }


    public interface ChannelOp {
        // 渠道开关与配置闭包 (唯一配置入口)
        ConfigOp email(Consumer<MailConfig> config);
        ConfigOp email();

        ConfigOp sms(Consumer<SmsConfig> config);

        ConfigOp sse(Consumer<SseConfig> config);
        ConfigOp sse(NotificationEventEnum eventEnum);
    }

    public interface ConfigOp extends ChannelOp{

        // 通用属性
        ConfigOp title(String title);

        ConfigOp withParam(String key, Object value);

        ConfigOp withExt(String key, Object value);

        TargetOp to();

    }

    public interface TargetOp {
        // 锁定目标类型的方法
        AllUserActionOp toAllUser();
        UserIdActionOp toUserIds(Long... userIds);
        UsernameActionOp toUsernames(String... usernames);
        EmailActionOp toEmails(String... emails);
        PhoneActionOp toPhones(String... phones);
    }

    public interface AllUserActionOp {
        NotificationDTO build();
    }

    public interface UserIdActionOp {
        UserIdActionOp toUserIds(Long... userIds);   // 仅允许继续加用户
        NotificationDTO build();
    }

    public interface UsernameActionOp {
        UsernameActionOp toUsernames(String... usernames);   // 仅允许继续加用户
        NotificationDTO build();
    }

    public interface EmailActionOp {
        EmailActionOp toEmails(String... emails); // 仅允许继续加邮箱
        NotificationDTO build();
    }

    public interface PhoneActionOp {
        PhoneActionOp toPhones(String... phones); // 仅允许继续加手机
        NotificationDTO build();
    }

    private record NotificationBuilder(NotificationRequest ctx) implements ChannelOp,ConfigOp {

        @Override
        public ConfigOp title(String title) {
            ctx.title = title;
            return this;
        }

        @Override
        public ConfigOp withParam(String key, Object value) {
            ctx.params = ensureMutable(ctx.params);
            ctx.params.put(key, value);
            return this;
        }

        @Override
        public ConfigOp withExt(String key, Object value) {
            ctx.ext = ensureMutable(ctx.ext);
            ctx.ext.put(key, value);
            return this;
        }

        @Override
        public ConfigOp email(Consumer<MailConfig> config) {
            enableChannel(ChannelType.EMAIL);
            config.accept(new MailConfig(this));
            return this;
        }

        @Override
        public ConfigOp email() {
            enableChannel(ChannelType.EMAIL);
            return this;
        }

        @Override
        public ConfigOp sms(Consumer<SmsConfig> config) {
            enableChannel(ChannelType.SMS);
            config.accept(new SmsConfig(this));
            return this;
        }

        @Override
        public ConfigOp sse(Consumer<SseConfig> config) {
            enableChannel(ChannelType.SSE);
            config.accept(new SseConfig(this));
            return this;
        }

        @Override
        public ConfigOp sse(NotificationEventEnum eventEnum) {
            enableChannel(ChannelType.SSE);
            withExt(NotificationConstant.Sse.EVENT_NAME, eventEnum.getCode());
            return this;
        }

        @Override
        public TargetOp to() {
            return new TargetOpBuilder(ctx);
        }

        private ConfigOp enableChannel(ChannelType type) {
            ctx.channels = ensureMutable(ctx.channels);
            if (!ctx.channels.contains(type)) {
                ctx.channels.add(type);
            }
            return this;
        }
    }

    private record TargetOpBuilder(NotificationRequest ctx) implements TargetOp,AllUserActionOp,UserIdActionOp,UsernameActionOp,EmailActionOp,PhoneActionOp {

        @Override
        public AllUserActionOp toAllUser() {
            ctx.targetType = TargetType.ALL;
            ctx.targets = new ArrayList<>();
            return this;
        }

        @Override
        public UserIdActionOp toUserIds(Long... userIds) {
            if (userIds == null || userIds.length == 0) {
                throw new NotificationException("接收目标不能为空");
            }
            String[] userIdList = Arrays.stream(userIds)
                    .map(String::valueOf)
                    .toArray(String[]::new);
            addTargets(TargetType.USER_ID, userIdList);
            return this;
        }

        @Override
        public UsernameActionOp toUsernames(String... usernames) {
            addTargets(TargetType.USER_ID, usernames);
            return this;
        }

        @Override
        public EmailActionOp toEmails(String... emails) {
            addTargets(TargetType.EMAIL, emails);
            return this;
        }

        @Override
        public PhoneActionOp toPhones(String... phones) {
            addTargets(TargetType.PHONE, phones);
            return this;
        }

        @Override
        public NotificationDTO build() {
            if (ctx.title == null || ctx.title.isBlank()) {
                ctx.title = "系统通知";
            }
            return NotificationDTO
                    .builder()
                    .templateCode(ctx.templateCode)
                    .title(ctx.title)
                    .text(ctx.text)
                    .contentData(ctx.contentData)
                    .targets(ctx.targets)
                    .targetType(ctx.targetType)
                    .channels(ctx.channels)
                    .params(ctx.params)
                    .ext(ctx.ext)
                    .build();
        }

        private void checkTargetType(TargetType newType) {
            if (ctx.targetType != null && ctx.targetType != newType) {
                throw new IllegalArgumentException(
                        String.format("MessageContext 意图冲突：当前目标类型为 %s，无法切换为 %s。请为一个 Context 保持单一目标类型。",
                                ctx.targetType, newType)
                );
            }
        }

        private void addTargets(TargetType type, String... newTargets) {
            if (newTargets == null || newTargets.length == 0) {
                throw new NotificationException("接收目标不能为空");
            }
            checkTargetType(type);
            ctx.targetType = type;

            // 初始化集合并保持追加顺序 (使用 LinkedHashSet 去重)
            Set<String> set = new LinkedHashSet<>();
            // 如果已有数据，先放进 set
            if (ctx.targets != null && !ctx.targets.isEmpty()) {
                set.addAll(ctx.targets);
            }
            // 追加新数据 (过滤掉 null 和 空字符串)
            for (String target : newTargets) {
                if (target != null && !target.isBlank()) {
                    set.add(target);
                }
            }
            // 写回 ctx (保持为可变的 ArrayList)
            ctx.targets = new ArrayList<>(set);
        }
    }

    public static class MailConfig {

        private final NotificationBuilder builder;

        private MailConfig(NotificationBuilder builder) {
            this.builder = builder;
        }

        // 自定义发件人
        public MailConfig from(String from) {
            builder.withExt(NotificationConstant.Mail.FROM, from);
            return this;
        }

        // 抄送
        public MailConfig cc(String... cc) {
            builder.withExt(NotificationConstant.Mail.CC, cc);
            return this;
        }

        // 密送
        public MailConfig bcc(String... bcc) {
            builder.withExt(NotificationConstant.Mail.BCC, bcc);
            return this;
        }

        // 指定回复地址
        public MailConfig replyTo(String replyTo) {
            builder.withExt(NotificationConstant.Mail.REPLY_TO, replyTo);
            return this;
        }

        // 邮件优先级 (1: 最高, 3: 普通, 5: 最低)
        public MailConfig priority(int priority) {
            builder.withExt(NotificationConstant.Mail.PRIORITY, priority);
            return this;
        }

        // 内嵌图片到 HTML 正文中
        @SuppressWarnings("unchecked")
        public MailConfig inlineImage(String cid, Object src) {
            if(src instanceof InputStream is){
                try (is) {
                    src = is.readAllBytes();
                } catch (IOException e) {
                    throw new NotificationException(e);
                }
            }
            // 确保 ext 整个 Map 不为 null 且可写
            builder.ctx.ext = ensureMutable(builder.ctx.getExt());
            // 获取或初始化附件 Map
            Map<String, Object> images = (Map<String, Object>) builder.ctx.ext.computeIfAbsent(
                    NotificationConstant.Mail.INLINE_IMAGES, k -> new HashMap<String, Object>()
            );
            images.put(cid, src);
            return this;
        }

        @SuppressWarnings("unchecked")
        public MailConfig attachment(String name, Object src) {
            if(src instanceof InputStream is){
                try (is) {
                    src = is.readAllBytes();
                } catch (IOException e) {
                    throw new NotificationException(e);
                }
            }
            // 确保 ext 整个 Map 不为 null 且可写
            builder.ctx.ext = ensureMutable(builder.ctx.getExt());
            // 获取或初始化附件 Map
            Map<String, Object> attachments = (Map<String, Object>) builder.ctx.ext.computeIfAbsent(
                    NotificationConstant.Mail.ATTACHMENTS, k -> new HashMap<String, Object>()
            );
            attachments.put(name, src);
            return this;
        }
    }

    public static class SseConfig {

        private final NotificationBuilder builder;

        private SseConfig(NotificationBuilder builder) {
            this.builder = builder;
        }

        public SseConfig eventName(NotificationEventEnum eventEnum) {
            builder.withExt(NotificationConstant.Sse.EVENT_NAME, eventEnum.getCode());
            return this;
        }
    }

    public static class SmsConfig {
        private final NotificationBuilder builder;

        private SmsConfig(NotificationBuilder builder) {
            this.builder = builder;
        }

        public SmsConfig signature(String signature) {
            builder.withExt(NotificationConstant.Sms.SIGNATURE, signature);
            return this;
        }

        // 如果数据库配置了 则会覆盖 以手动指定的优先
        public SmsConfig extTemplateCode(String extCode) {
            builder.withExt(NotificationConstant.Sms.EXT_TEMPLATE_CODE, extCode);
            return this;
        }

    }

    private static <T> List<T> ensureMutable(List<T> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        // 如果已经是 ArrayList，直接返回原对象，不再 new
        if (list instanceof ArrayList) {
            return list;
        }
        // 否则（如 List.of 或 Arrays.asList），才进行转换
        return new ArrayList<>(list);
    }

    private static <K, V> Map<K, V> ensureMutable(Map<K, V> map) {
        if (map == null) {
            return new HashMap<>();
        }
        // 如果已经是 HashMap，直接返回原对象
        if (map instanceof HashMap) {
            return map;
        }
        // 否则（如 Map.of 或 SingletonMap），进行转换
        return new HashMap<>(map);
    }

}
