package com.atlas.auth.service.provider;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.CasProviderSettings;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    public CasAuthenticationSuccess validateTicket(String provider, CasProviderSettings settings, String ticket) {
        if (settings == null
                || settings.serviceValidateUrl() == null
                || settings.serviceUrl() == null
                || ticket == null
                || ticket.isBlank()) {
            throw new CasProviderException("Invalid CAS ticket validation parameters");
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(settings.serviceValidateUrl())
                    .queryParam("ticket", ticket)
                    .queryParam("service", settings.serviceUrl())
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

}
