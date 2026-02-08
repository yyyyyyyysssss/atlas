package com.atlas.notification.service;

import com.atlas.common.api.constant.NotificationConstant;
import com.atlas.common.api.enums.ChannelType;
import com.atlas.common.api.exception.NotificationException;
import com.atlas.notification.domain.mode.HtmlPayload;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.TextPayload;
import com.atlas.notification.enums.NotificationErrorCode;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 14:07
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailMessageAdapter extends AbstractMessageAdapter implements MessageAdapter {

    private final JavaMailSender javaMailSender;

    private final RestClient defaultRestClient;

    @Value("${spring.mail.from:''}")
    private String from;

    private static final long MAX_RESOURCE_SIZE = 1024 * 1024 * 20;

    @Override
    public boolean support(ChannelType channelType) {

        return channelType == ChannelType.EMAIL;
    }

    @Override
    public void send(MessagePayload payload, List<String> targets) {
        StopWatch sw = new StopWatch("Email-Sender");
        String subject = Optional.ofNullable(payload.getTitle()).orElse("Untitled");
        // 对目标地址进行简化展示，防止长列表刷屏
        String targetSummary = targets.size() > 5 ? targets.subList(0, 5) + "...total " + targets.size() : targets.toString();
        String logId = String.format("Subject: [%s] To: %s", subject, targetSummary);
        try {
            // 阶段1: 设置邮件基础参数
            sw.start("Build-MimeMessage");
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            // true 表示支持多路径/附件/HTML
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            // 标题
            helper.setSubject(subject);
            // 接收人
            helper.setTo(targets.toArray(new String[0]));
            Map<String, Object> ext = Optional.ofNullable(payload.getExt()).orElse(Collections.emptyMap());
            // 发件人
            String mailFrom = getAsString(ext, NotificationConstant.Mail.FROM, from);
            if(StringUtils.isNotEmpty(mailFrom)){
                helper.setFrom(mailFrom);
            }
            // 抄送人
            String[] cc = getAsArray(ext,NotificationConstant.Mail.CC);
            if(cc.length > 0){
                helper.setCc(cc);
            }
            //密送
            String[] bcc = getAsArray(ext,NotificationConstant.Mail.BCC);
            if(bcc.length > 0){
                helper.setBcc(bcc);
            }
            // 指定回复地址
            String replyTo = getAsString(ext,NotificationConstant.Mail.REPLY_TO);
            if(StringUtils.isNotEmpty(replyTo)){
                helper.setReplyTo(replyTo);
            }
            // 优先级
            Integer priority = getAsInteger(ext,NotificationConstant.Mail.PRIORITY);
            if(priority != null){
                helper.setPriority(priority);
            }
            sw.stop();

            // 阶段2: 处理附件
            sw.start("Process-Attachments");
            processAttachments(helper, getAsMap(ext,NotificationConstant.Mail.ATTACHMENTS));
            sw.stop();

            // 阶段3: 渲染正文与内嵌图片
            sw.start("Render-Content");
            if (payload instanceof HtmlPayload htmlPayload) {
                helper.setText(htmlPayload.getHtml(), true);
                // 处理正文内嵌的图片
                processInlineImages(helper, getAsMap(ext, NotificationConstant.Mail.INLINE_IMAGES));
            } else if (payload instanceof TextPayload textPayload) {
                helper.setText(textPayload.getText(), false);
            }
            sw.stop();

            // 阶段4: 发送
            sw.start("SMTP-Send");
            javaMailSender.send(mimeMessage);
            sw.stop();

            log.info("[Email-Sender] Send Success {} Cost: {}s", logId, String.format("%.3f", sw.getTotalTimeSeconds()));
        } catch (Exception e) {
            if(e instanceof NotificationException ne){
                throw ne;
            }
            throw new NotificationException(e);
        } finally {
            if(sw.isRunning()){
                sw.stop();
            }
            if (log.isDebugEnabled()) {
                log.debug("\n{}", sw.prettyPrint());
            }
        }
    }

    private void processAttachments(MimeMessageHelper helper, Map<String, Object> attachments) throws Exception {
        if (attachments.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            String fileName = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            Resource resource = resolveResource(fileName, entry.getValue());
            if(resource == null){
                throw new NotificationException(NotificationErrorCode.NOTIFY_RESOURCE_NOT_FOUND, " Resource: " + fileName);
            }
            helper.addAttachment(fileName, resource);
            if (log.isDebugEnabled()) {
                log.debug("[Email-Attachment] Added File: {}", fileName);
            }
        }
    }

    private void processInlineImages(MimeMessageHelper helper, Map<String, Object> images) throws Exception {
        if (images.isEmpty()){
            return;
        }
        ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
        for (Map.Entry<String, Object> entry : images.entrySet()) {
            String cid = entry.getKey();
            Object src = entry.getValue();
            if (src == null){
                continue;
            }
            Resource resource = resolveResource(cid, entry.getValue());
            if (resource != null) {
                String contentType = fileTypeMap.getContentType(cid);
                // 兜底逻辑：如果还是识别不出，强制指定为 image/png
                if (!contentType.startsWith("image/")) {
                    contentType = "image/png";
                }
                helper.addInline(cid, resource, contentType);
                if (log.isDebugEnabled()) {
                    log.debug("[Email-Inline] Added CID: {}", cid);
                }
            }
        }
    }

    private Resource resolveResource(String identifier, Object src) {
        if (src == null){
            throw new NullPointerException("src is null");
        }
        long start = System.currentTimeMillis();
        String type = "Unknown";
        long size = 0;
        Resource resource =  switch (src) {
            case byte[] bytes -> { // 字节数组
                type = "Bytes-Array";
                size = bytes.length;
                validateSize(bytes.length, identifier, MAX_RESOURCE_SIZE);
                yield new ByteArrayResource(bytes);
            }
            case String path when path.startsWith("http") -> {
                byte[] data = downloadWithValidation(path, MAX_RESOURCE_SIZE);
                type = "Remote-URL";
                size = data.length;
                yield new ByteArrayResource(data);
            }
            case String base64Str -> {
                // base64
                byte[] decoded = Base64.getDecoder().decode(base64Str);
                validateSize(decoded.length, identifier, MAX_RESOURCE_SIZE);
                type = "Base64";
                yield new ByteArrayResource(decoded);
            }
            default -> {
                log.warn("[Email-Resource] resource type: {} for {}", identifier, src.getClass().getSimpleName());
                yield null;
            }
        };
        if (resource != null) {
            long cost = System.currentTimeMillis() - start;
            if (log.isDebugEnabled()) {
                log.debug("[Email-Resource] Resolved. ID: {}, Type: {}, Size: {}, Cost: {}ms", identifier, type, formatSize(size), cost);
            }
        }
        return resource;
    }

    /**
     * 带有大小预检和超时控制的下载逻辑
     */
    private byte[] downloadWithValidation(String urlPath, long maxSize) {

        return defaultRestClient.get()
                .uri(urlPath)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new NotificationException(NotificationErrorCode.NOTIFY_RESOURCE_UNAVAILABLE);
                    }
                    long contentLength = response.getHeaders().getContentLength();
                    if (contentLength > maxSize) {
                        throw new NotificationException(NotificationErrorCode.NOTIFY_RESOURCE_RESTRICTED);
                    }
                    try (InputStream is = response.getBody(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                        byte[] buffer = new byte[8192];
                        int n;
                        long totalRead = 0;
                        while ((n = is.read(buffer)) != -1) {
                            totalRead += n;
                            if (totalRead > maxSize) {
                                throw new NotificationException(NotificationErrorCode.NOTIFY_RESOURCE_RESTRICTED);
                            }
                            baos.write(buffer, 0, n);
                        }
                        return baos.toByteArray();
                    }
                });
    }

    private void validateSize(long size, String name, long maxSize) {
        if (size > maxSize) {
            throw new NotificationException(NotificationErrorCode.NOTIFY_RESOURCE_RESTRICTED);
        }
    }
}
