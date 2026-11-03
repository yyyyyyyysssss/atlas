package com.atlas.auth.config.security.cas;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.UserInfo;
import com.atlas.auth.domain.entity.CasRegisteredClient;
import com.atlas.auth.service.CasRegisteredClientService;
import com.atlas.auth.service.UserInfoService;
import com.atlas.common.crypto.digest.DigestUtils;
import com.atlas.common.crypto.random.SecureRandomUtils;
import com.atlas.common.redis.utils.RedisHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/28 11:24
 */
@Component
@Slf4j
public class CasService {


    private final RedisHelper redisHelper;

    private final UserInfoService userInfoService;

    private final CasRegisteredClientService casRegisteredClientService;

    private static final String KEY_PREFIX = "cas:ticket:";

    /**
     * CAS Service Ticket 有效期。
     */
    private static final Duration TICKET_TTL = Duration.ofSeconds(300);

    private static final String CAS_NAMESPACE = "http://www.yale.edu/tp/cas";

    private static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_.-]*");


    public CasService(RedisHelper redisHelper, UserInfoService userInfoService, CasRegisteredClientService casRegisteredClientService){
        this.redisHelper = redisHelper;
        this.userInfoService = userInfoService;
        this.casRegisteredClientService = casRegisteredClientService;
    }


    // 签发 CAS Service Ticket。
    public String issueServiceTicket(String service, Long userId) {
        if (service == null || service.isBlank() || userId == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, service);
        }
        String ticket = "ST-" + SecureRandomUtils.generate(32);
        String key = buildKey(ticket, service);
        boolean success = redisHelper.setIfAbsent(key, userId, TICKET_TTL);
        if (!success) {
            throw new AuthServiceException("CAS Service Ticket already exists");
        }
        return ticket;
    }

    /**
     * 验证 CAS Service Ticket。
     *
     * @param ticket  Service Ticket
     * @param service 客户端 Service URL
     * @param version CAS 协议版本
     * @return CAS XML 响应
     */
    public String validateServiceTicket(String ticket, String service, CasProtocolVersion version) {
        if (version == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, service);
        }

        CasRegisteredClient casRegisteredClient = casRegisteredClientService.getEnabledByService(service);
        if(casRegisteredClient == null){
            return createServiceFailureResponse("INVALID_SERVICE", "Service 无效或不匹配");
        }

        // 验证并消费 Service Ticket
        Long userId = consumeServiceTicket(ticket, service);

        if (userId == null) {
            return createServiceFailureResponse("INVALID_TICKET", "Ticket 不存在、已过期或已被使用");
        }
        UserInfo userInfo = userInfoService.loadByUserId(userId);
        if (userInfo == null) {
            return createServiceFailureResponse("INVALID_TICKET", "Ticket 对应的用户不存在");
        }
        return createServiceResponse(userInfo, casRegisteredClient, version);
    }

    // 消费 CAS Service Ticket。
    private Long consumeServiceTicket(String ticket, String service) {
        if (ticket == null || ticket.isBlank() || service == null || service.isBlank()) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, service);
        }
        String key = buildKey(ticket, service);
        return redisHelper.getValueAndDelete(key, Long.class);
    }

    /**
     * 创建 CAS Service Ticket 验证成功响应。
     * CAS 2.0：返回 user。
     * CAS 3.0：返回 user 和 attributes。
     */
    private String createServiceResponse(UserInfo userInfo, CasRegisteredClient casRegisteredClient, CasProtocolVersion version) {
        try {
            StringWriter writer = new StringWriter();
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(writer);
            xml.writeStartDocument("UTF-8", "1.0");

            // serviceResponse
            xml.writeStartElement("cas", "serviceResponse", CAS_NAMESPACE);
            xml.writeNamespace("cas", CAS_NAMESPACE);

            // authenticationSuccess
            xml.writeStartElement("cas", "authenticationSuccess", CAS_NAMESPACE);

            // user
            xml.writeStartElement("cas", "user", CAS_NAMESPACE);
            xml.writeCharacters(userInfo.sub());
            xml.writeEndElement();

            // CAS 3.0 返回用户属性
            if (version == CasProtocolVersion.CAS_3) {
                writeAttributes(xml, userInfo, casRegisteredClient);
            }

            xml.writeEndElement();
            xml.writeEndElement();

            xml.writeEndDocument();
            xml.close();

            return writer.toString();
        } catch (XMLStreamException e) {
            throw new AuthServiceException("CAS XML response generation failed", e);
        }
    }

    /**
     * 创建 CAS Service Ticket 验证失败响应
     *
     * @param errorCode   CAS 协议错误代码
     * @param description 错误描述
     */
    private String createServiceFailureResponse(String errorCode, String description) {
        try {
            StringWriter writer = new StringWriter();

            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(writer);

            xml.writeStartDocument("UTF-8", "1.0");

            // serviceResponse
            xml.writeStartElement("cas", "serviceResponse", CAS_NAMESPACE);
            xml.writeNamespace("cas", CAS_NAMESPACE);

            // authenticationFailure
            xml.writeStartElement("cas", "authenticationFailure", CAS_NAMESPACE);

            xml.writeAttribute("code", errorCode);

            if (description != null) {
                xml.writeCharacters(description);
            }

            xml.writeEndElement();
            xml.writeEndElement();

            xml.writeEndDocument();
            xml.close();

            return writer.toString();

        } catch (XMLStreamException e) {
            throw new AuthServiceException("CAS XML response generation failed", e);
        }
    }


    /**
     * 写入 CAS 3.0 用户属性。
     */
    private void writeAttributes(XMLStreamWriter xml, UserInfo userInfo, CasRegisteredClient casRegisteredClient) throws XMLStreamException {
        Map<String, Object> attributes = userInfo.attributes();

        Map<String,String> mappings = Optional.ofNullable(casRegisteredClient.getAttributeMappings()).orElse(Collections.emptyMap());

        xml.writeStartElement("cas", "attributes", CAS_NAMESPACE);

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value == null || value.toString().isBlank()) {
                continue;
            }

            String attributeName = mappings.getOrDefault(name, name);

            if (attributeName == null || !ATTRIBUTE_NAME_PATTERN.matcher(attributeName).matches()) {
                log.warn("Invalid CAS attribute mapping, source: {}, target: {}", name, attributeName);
                continue;
            }

            xml.writeStartElement("cas", attributeName, CAS_NAMESPACE);
            xml.writeCharacters(value.toString());
            xml.writeEndElement();
        }


        xml.writeEndElement();
    }


    private String buildKey(String ticket, String service) {
        return KEY_PREFIX + ticket + ":" + DigestUtils.sha256(service);
    }

}
