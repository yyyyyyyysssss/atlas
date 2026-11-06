package com.atlas.auth.service.provider;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.CasProviderSettings;
import com.atlas.common.redis.utils.RedisHelper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/4 15:01
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class CasProviderEngine extends AbstractProviderEngine {

    private static final String CAS_NAMESPACE = "http://www.yale.edu/tp/cas";

    private final RedisHelper redisHelper;

    private static final XmlMapper XML_MAPPER = new XmlMapper();


    public CasAuthenticationSuccess validateServerTicket(String provider, CasProviderSettings settings, String ticket, String pgtUrl) {

        return validateTicket(provider, settings, ticket, settings.serviceValidateUrl(), pgtUrl);
    }

    public CasAuthenticationSuccess validateProxyTicket(String provider, CasProviderSettings settings, String ticket) {

        return validateTicket(provider, settings, ticket, settings.proxyValidateUrl(), null);
    }

    public String getProxyTicket(String provider, CasProviderSettings settings, String pgt, String targetService) {
        if (settings == null
                || settings.proxyUrl() == null || settings.proxyUrl().isBlank()
                || pgt == null || pgt.isBlank()
                || targetService == null || targetService.isBlank()) {
            throw new CasProviderException("Invalid CAS proxy ticket request parameters");
        }
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(settings.proxyUrl())
                    .queryParam("pgt", pgt)
                    .queryParam("targetService", targetService)
                    .build()
                    .encode()
                    .toUri();

            String response = getClient(provider)
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (response.isBlank()) {
                throw new CasProviderException("Empty CAS proxy response");
            }

            CasProxyResponse result = XML_MAPPER.readValue(response, CasProxyResponse.class);

            if (result == null) {
                throw new CasProviderException("Invalid CAS proxy response");
            }

            if (result.getFailure() != null) {
                throw new CasProviderException(
                        "CAS proxy ticket request failed: "
                                + result.getFailure().getCode()
                                + ", "
                                + result.getFailure().getMessage()
                );
            }

            CasProxySuccess success = result.getSuccess();

            if (success == null
                    || success.getProxyTicket() == null
                    || success.getProxyTicket().isBlank()) {
                throw new CasProviderException("Invalid CAS proxy success response");
            }

            return success.getProxyTicket();

        } catch (CasProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("CAS proxy ticket request failed, provider: {}", provider, e);
            throw new CasProviderException("CAS proxy ticket request failed", e);
        }
    }

    private CasAuthenticationSuccess validateTicket(String provider, CasProviderSettings settings, String ticket, String validateUrl, String pgtUrl) {
        if (settings == null
                || validateUrl == null || validateUrl.isBlank()
                || settings.serviceUrl() == null || settings.serviceUrl().isBlank()
                || ticket == null || ticket.isBlank()) {
            throw new CasProviderException("Invalid CAS ticket validation parameters");
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(validateUrl)
                    .queryParam("ticket", ticket)
                    .queryParam("service", settings.serviceUrl())
                    .queryParamIfPresent("pgtUrl", StringUtils.hasText(pgtUrl) ? Optional.of(pgtUrl) : Optional.empty())
                    .build()
                    .encode()
                    .toUri();

            RestClient restClient = getClient(provider);
            String response = restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            if (response.isBlank()) {
                throw new CasProviderException("Empty CAS validation response");
            }
            // 解析 CAS XML 响应。
            CasServiceResponse result = XML_MAPPER.readValue(response, CasServiceResponse.class);
            if (result == null) {
                throw new CasProviderException("Invalid CAS validation response");
            }
            // 认证失败。
            if (result.getFailure() != null) {
                throw new CasProviderException("CAS authentication failed: "
                        + result.getFailure().getCode()
                        + ", "
                        + result.getFailure().getMessage()
                );
            }
            // 认证成功。
            CasAuthenticationSuccess success = result.getSuccess();
            if (success == null || success.getUser() == null || success.getUser().isBlank()) {
                throw new CasProviderException("Invalid CAS authentication success response");
            }
            return success;
        } catch (CasProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("CAS ticket validation failed, provider: {}", provider, e);
            throw new CasProviderException("CAS ticket validation request failed", e);
        }
    }

    public void pgtCallback(String pgtId, String pgtIou) {
        if (pgtId == null || pgtId.isBlank()
                || pgtIou == null || pgtIou.isBlank()) {
            throw new CasProviderException("Invalid PGT callback parameters");
        }
        String key = buildPgtKey(pgtIou);
        // 保存 PGTIOU -> PGT 映射，有效期与 CAS Server 的 PGT 有效期约定一致
        boolean success = redisHelper.setIfAbsent(key, pgtId, Duration.ofHours(8));
        if (!success) {
            throw new CasProviderException("Duplicate PGTIOU callback");
        }
    }

    // 根据 PGTIOU 获取 PGT
    public String getProxyGrantingTicket(String pgtIou) {
        if (pgtIou == null || pgtIou.isBlank()) {
            return null;
        }

        return redisHelper.getValue(buildPgtKey(pgtIou), String.class);
    }

    private String buildPgtKey(String pgtIou) {
        return "cas:client:pgt:" + ":" + pgtIou;
    }


    public static class CasProviderException extends AuthServiceException {

        public CasProviderException(String message) {
            super(message);
        }

        public CasProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // CAS XML 响应根节点。
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JacksonXmlRootElement(localName = "serviceResponse", namespace = CAS_NAMESPACE)
    @Getter
    @Setter
    public static class CasServiceResponse {

        @JacksonXmlProperty(localName = "authenticationSuccess", namespace = CAS_NAMESPACE)
        private CasAuthenticationSuccess success;

        @JacksonXmlProperty(localName = "authenticationFailure", namespace = CAS_NAMESPACE)
        private CasAuthenticationFailure failure;
    }

    // CAS 认证成功响应。
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class CasAuthenticationSuccess {

        @JacksonXmlProperty(localName = "user", namespace = CAS_NAMESPACE)
        private String user;

        @JacksonXmlProperty(localName = "attributes", namespace = CAS_NAMESPACE)
        private Map<String, Object> attributes = new LinkedHashMap<>();

        @JacksonXmlProperty(localName = "proxyGrantingTicket", namespace = CAS_NAMESPACE)
        private String proxyGrantingTicket;
    }

    // CAS 认证失败响应。
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class CasAuthenticationFailure {

        @JacksonXmlProperty(isAttribute = true, localName = "code")
        private String code;

        @JacksonXmlText
        private String message;
    }


    @Getter
    @JacksonXmlRootElement(
            localName = "serviceResponse",
            namespace = CAS_NAMESPACE
    )
    public static class CasProxyResponse {

        @JacksonXmlProperty(
                localName = "proxySuccess",
                namespace = CAS_NAMESPACE
        )
        private CasProxySuccess success;

        @JacksonXmlProperty(
                localName = "proxyFailure",
                namespace = CAS_NAMESPACE
        )
        private CasAuthenticationFailure failure;
    }

    @Getter
    public static class CasProxySuccess {

        @JacksonXmlProperty(
                localName = "proxyTicket",
                namespace = CAS_NAMESPACE
        )
        private String proxyTicket;
    }

}
