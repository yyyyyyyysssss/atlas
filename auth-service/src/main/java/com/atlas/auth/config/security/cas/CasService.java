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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.*;
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

    private final CasProxyGrantingTicketRepository casProxyGrantingTicketRepository;

    private final RestClient restClient;

    private static final String KEY_PREFIX = "cas:ticket:";

    private static final String CAS_NAMESPACE = "http://www.yale.edu/tp/cas";

    private static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_.-]*");


    public CasService(RedisHelper redisHelper,
                      UserInfoService userInfoService,
                      CasRegisteredClientService casRegisteredClientService,
                      CasProxyGrantingTicketRepository casProxyGrantingTicketRepository,
                      RestClient defaultRestClient) {
        this.redisHelper = redisHelper;
        this.userInfoService = userInfoService;
        this.casRegisteredClientService = casRegisteredClientService;
        this.casProxyGrantingTicketRepository = casProxyGrantingTicketRepository;
        this.restClient = defaultRestClient;
    }


    // 签发 CAS Service Ticket。
    public String issueServiceTicket(String service, Long userId) {
        if (service == null || service.isBlank() || userId == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, service);
        }
        return issueTicket(CasTicketType.SERVICE, service, userId, List.of());
    }

    // 签发 CAS 代理 Ticket
    public String issueProxyTicket(String pgt, String targetService) {
        if (pgt == null || pgt.isBlank() || targetService == null || targetService.isBlank()) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, targetService);
        }
        // 查询 PGT，不消费，PGT 可以重复使用。
        CasProxyGrantingTicketContext pgtContext = casProxyGrantingTicketRepository.findByTicket(pgt);
        if (pgtContext == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_TICKET, pgt);
        }
        // 校验目标 Service 是否已注册且启用。
        CasRegisteredClient client = casRegisteredClientService.getEnabledByService(targetService);

        if (client == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_SERVICE, targetService);
        }

        // 构建代理链：原始 PGT 对应的代理链 + 当前代理 Service。
        List<String> proxies = new ArrayList<>(pgtContext.proxies());
        proxies.add(pgtContext.service());

        // 统一签发 PT。
        return issueTicket(CasTicketType.PROXY, targetService, pgtContext.userId(), proxies);
    }

    // 验证 CAS Service Ticket。
    public String validateServiceTicket(CasTicketValidationRequest validationRequest) {
        if (validationRequest == null || validationRequest.version() == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST);
        }
        String ticket = validationRequest.ticket();
        String service = validationRequest.service();
        CasProtocolVersion version = validationRequest.version();

        CasRegisteredClient casRegisteredClient = casRegisteredClientService.getEnabledByService(service);
        if (casRegisteredClient == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_SERVICE, service);
        }

        // 验证并消费 Service Ticket
        CasTicketContext context = consumeTicket(CasTicketType.SERVICE, ticket, service);

        if (context == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_TICKET, ticket);
        }
        UserInfo userInfo = userInfoService.loadByUserId(context.userId());
        if (userInfo == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_TICKET, ticket);
        }

        // 可选：申请 PGT
        String pgtIou = null;
        if (StringUtils.hasText(validationRequest.pgtUrl())) {
            pgtIou = issueProxyGrantingTicket(context.userId(), casRegisteredClient, validationRequest.pgtUrl());
        }

        return createServiceResponse(userInfo, casRegisteredClient, version, pgtIou, List.of());
    }

    // 验证 CAS Service Proxy Ticket。
    public String validateProxyTicket(CasTicketValidationRequest validationRequest) {
        if (validationRequest == null || validationRequest.version() == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST);
        }
        String ticket = validationRequest.ticket();
        String service = validationRequest.service();
        CasProtocolVersion version = validationRequest.version();
        // 校验目标 Service。
        CasRegisteredClient casRegisteredClient = casRegisteredClientService.getEnabledByService(service);
        if (casRegisteredClient == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_SERVICE, service);
        }
        // 验证并消费 Proxy Ticket。
        CasTicketContext context = consumeTicket(CasTicketType.PROXY, ticket, service);
        if (context == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_TICKET, ticket);
        }

        // 加载用户。
        UserInfo userInfo = userInfoService.loadByUserId(context.userId());

        if (userInfo == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_TICKET, ticket);
        }

        // 返回用户信息及代理链。
        return createServiceResponse(userInfo, casRegisteredClient, version, null, context.proxies());
    }

    // 签发 Proxy Granting Ticket
    private String issueProxyGrantingTicket(Long userId, CasRegisteredClient client, String pgtUrl) {
        // 检查客户端是否开启代理认证
        if (!Boolean.TRUE.equals(client.getProxyEnabled())) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, pgtUrl);
        }
        // 校验回调地址白名单
        List<String> callbackUrls = client.getProxyCallbackUrls();
        if (callbackUrls == null || !callbackUrls.contains(pgtUrl)) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, pgtUrl);
        }
        // 生成 PGT 和 PGTIOU
        String pgt = "PGT-" + SecureRandomUtils.generate(32); // 代理凭证
        String pgtIou = "PGTIOU-" + SecureRandomUtils.generate(32); // 关联标识
        // 构建 PGT 上下文
        CasProxyGrantingTicketContext context = new CasProxyGrantingTicketContext(
                pgt,
                userId,
                client.getService(),
                pgtUrl,
                List.of()
        );
        // 保存PGT
        casProxyGrantingTicketRepository.save(context);
        // 回调
        try {
            // 构建回调地址。
            URI callbackUri = UriComponentsBuilder
                    .fromUriString(pgtUrl)
                    .queryParam("pgtId", pgt)
                    .queryParam("pgtIou", pgtIou)
                    .build()
                    .encode()
                    .toUri();
            // 服务端主动调用客户端 PGT 回调接口。
            restClient.get()
                    .uri(callbackUri)
                    .retrieve()
                    .toBodilessEntity();
            return pgtIou;
        } catch (Exception e) {
            // 回调失败，删除刚刚签发的 PGT。
            casProxyGrantingTicketRepository.deleteByTicket(pgt);
            log.warn("CAS PGT callback failed, pgtUrl: {}", pgtUrl, e);
            return null;
        }

    }

    private String issueTicket(CasTicketType type, String service, Long userId, List<String> proxies) {
        if (type == null || !StringUtils.hasText(service) || userId == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, service);
        }
        String ticket = type.getPrefix() + SecureRandomUtils.generate(32);
        String key = buildKey(ticket, service);
        CasTicketContext context = new CasTicketContext(userId, proxies == null ? List.of() : List.copyOf(proxies));
        boolean success = redisHelper.setIfAbsent(key, context, type.getTtl());
        if (!success) {
            throw new AuthServiceException("CAS Service Ticket already exists");
        }
        return ticket;
    }

    // 消费 CAS Service Ticket。
    private CasTicketContext consumeTicket(CasTicketType type, String ticket, String service) {
        if (type == null || ticket == null || ticket.isBlank() || service == null || service.isBlank()) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, service);
        }
        // 校验票据类型前缀。
        if (!ticket.startsWith(type.getPrefix())) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_TICKET, ticket);
        }
        String key = buildKey(ticket, service);
        return redisHelper.getValueAndDelete(key, CasTicketContext.class);
    }

    /**
     * 创建 CAS Service Ticket 验证成功响应。
     * CAS 2.0：返回 user。
     * CAS 3.0：返回 user 和 attributes。
     */
    private String createServiceResponse(UserInfo userInfo, CasRegisteredClient casRegisteredClient, CasProtocolVersion version, String pgtIou, List<String> proxies) {
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

            // PGTIOU
            if (StringUtils.hasText(pgtIou)) {
                xml.writeStartElement("cas", "proxyGrantingTicket", CAS_NAMESPACE);
                xml.writeCharacters(pgtIou);
                xml.writeEndElement();
            }

            // Proxies（PT 验证时返回）
            if (proxies != null && !proxies.isEmpty()) {
                xml.writeStartElement("cas", "proxies", CAS_NAMESPACE);
                for (String proxy : proxies) {
                    xml.writeStartElement("cas", "proxy", CAS_NAMESPACE);
                    xml.writeCharacters(proxy);
                    xml.writeEndElement();
                }

                xml.writeEndElement();
            }

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

    public String createAuthenticationFailureResponse(CasAuthenticationException e) {

        return createFailureResponse("authenticationFailure", e.getAuthErrorCode().getMessage(), e.getMessage());
    }

    public String createProxyFailureResponse(CasAuthenticationException e) {

        return createFailureResponse("proxyFailure", e.getAuthErrorCode().getMessage(), e.getMessage());
    }

    private String createFailureResponse(String failureElement, String errorCode, String description) {

        try {
            StringWriter writer = new StringWriter();

            XMLStreamWriter xml = XMLOutputFactory
                    .newFactory()
                    .createXMLStreamWriter(writer);

            xml.writeStartDocument("UTF-8", "1.0");

            xml.writeStartElement("cas", "serviceResponse", CAS_NAMESPACE);
            xml.writeNamespace("cas", CAS_NAMESPACE);

            xml.writeStartElement("cas", failureElement, CAS_NAMESPACE);
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

    // 创建 CAS Proxy Ticket 签发成功响应。
    public String createProxyResponse(String proxyTicket) {
        try {
            StringWriter writer = new StringWriter();
            XMLStreamWriter xml = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(writer);

            xml.writeStartDocument("UTF-8", "1.0");

            // serviceResponse
            xml.writeStartElement("cas", "serviceResponse", CAS_NAMESPACE);
            xml.writeNamespace("cas", CAS_NAMESPACE);

            // proxySuccess
            xml.writeStartElement("cas", "proxySuccess", CAS_NAMESPACE);

            // proxyTicket
            xml.writeStartElement("cas", "proxyTicket", CAS_NAMESPACE);
            xml.writeCharacters(proxyTicket);
            xml.writeEndElement();

            xml.writeEndElement();
            xml.writeEndElement();

            xml.writeEndDocument();
            xml.close();

            return writer.toString();
        } catch (XMLStreamException e) {
            throw new AuthServiceException("CAS Proxy response generation failed", e);
        }
    }


    /**
     * 写入 CAS 3.0 用户属性。
     */
    private void writeAttributes(XMLStreamWriter xml, UserInfo userInfo, CasRegisteredClient casRegisteredClient) throws XMLStreamException {
        Map<String, Object> attributes = userInfo.attributes();

        Map<String, String> mappings = Optional.ofNullable(casRegisteredClient.getAttributeMappings()).orElse(Collections.emptyMap());

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
