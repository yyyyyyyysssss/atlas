package com.atlas.notification.adapter;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.atlas.common.core.api.notification.constant.NotificationConstant;
import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.exception.NotificationException;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.notification.config.properties.SmsProperties;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.enums.NotificationErrorCode;
import darabonba.core.client.ClientOverrideConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 15:30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SMSMessageAdapter extends AbstractMessageAdapter implements MessageAdapter {

    private final SmsProperties smsProperties;

    private AsyncClient asyncClient;

    @PostConstruct
    public void init() {
        StaticCredentialProvider provider = StaticCredentialProvider.create(
                Credential.builder()
                        .accessKeyId(smsProperties.getAliyun().getAccessKey())
                        .accessKeySecret(smsProperties.getAliyun().getAccessSecret())
                        .build()
        );
        this.asyncClient = AsyncClient.builder()
                .credentialsProvider(provider)
                .overrideConfiguration(ClientOverrideConfiguration.create()
                        .setEndpointOverride(smsProperties.getAliyun().getEndpoint()))
                .build();
    }

    @Override
    public boolean support(ChannelType channelType) {
        return channelType == ChannelType.SMS;
    }

    @Override
    public void send(MessagePayload payload, List<String> targets) {

        StopWatch sw = new StopWatch("SMS-Sender");
        sw.start();

        Map<String, Object> params = payload.getParams();

        Map<String, Object> ext = Optional.ofNullable(payload.getExt()).orElse(Collections.emptyMap());
        // 外部服务商模板编码 以手动指定的优先
        String manualExtCode = getAsString(ext, NotificationConstant.Sms.EXT_TEMPLATE_CODE);
        String modelExtCode = payload.getExtTemplateCode();
        String finalExtTemplateCode = StringUtils.defaultIfBlank(manualExtCode, modelExtCode);

        // 目标手机号
        String phoneNumbers = String.join(",", targets);

        String logId = String.format("ExtTemplateCode: [%s] To: %s", finalExtTemplateCode, phoneNumbers);

        // 签名名称 以手动指定的优先
        String signature = getAsString(ext, NotificationConstant.Sms.SIGNATURE, smsProperties.getAliyun().getSignature());

        try {
            SendSmsVerifyCodeRequest sendSmsVerifyCodeRequest = SendSmsVerifyCodeRequest.builder()
                    .templateCode(finalExtTemplateCode)
                    .signName(signature)
                    .phoneNumber(phoneNumbers)
                    .templateParam(JsonUtils.toJson(params))
                    .build();
            CompletableFuture<SendSmsVerifyCodeResponse> response = asyncClient.sendSmsVerifyCode(sendSmsVerifyCodeRequest);
            SendSmsVerifyCodeResponse resp = response.get(smsProperties.getAliyun().getTimeout(), TimeUnit.SECONDS);
            SendSmsVerifyCodeResponseBody respBody = resp.getBody();

            sw.stop();
            if(!respBody.getSuccess() || !"OK".equalsIgnoreCase(respBody.getCode())){
                String respResult = JsonUtils.toJson(respBody);
                throw new NotificationException(NotificationErrorCode.CHANNEL_BIZ_ERROR, respResult);
            }
            log.info("[SMS-Sender] Send Success {} Cost: {}s", logId, String.format("%.3f", sw.getTotalTimeSeconds()));
        } catch (Exception e) {
            if(e instanceof NotificationException ne){
                throw ne;
            }
            throw new NotificationException(e);
        } finally {
            if (sw.isRunning()) {
                sw.stop();
            }
            if (log.isDebugEnabled()) {
                log.debug("\n{}", sw.prettyPrint());
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (this.asyncClient != null) {
            this.asyncClient.close();
        }
    }

}
