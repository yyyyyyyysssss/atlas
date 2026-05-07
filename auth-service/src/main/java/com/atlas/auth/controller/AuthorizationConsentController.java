package com.atlas.auth.controller;

import com.atlas.auth.config.security.oauth2.CustomClientSettings;
import com.atlas.security.properties.SecurityProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/7 16:46
 */
@Controller
@Slf4j
public class AuthorizationConsentController {

    @Resource
    private RegisteredClientRepository registeredClientRepository;

    @Resource
    private SecurityProperties securityProperties;

    //自定义授权同意页面x
    @GetMapping(value = "/oauth2/consent")
    public String consent(@RequestParam("type") String type,
                          @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
                          @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
                          @RequestParam(OAuth2ParameterNames.STATE) String state,
                          @RequestParam(value = OAuth2ParameterNames.USER_CODE,required = false) String userCode
    ) {
        log.info("type:{},clientId:{},scope:{},state:{},userCode:{}",type,clientId,scope,state,userCode);
        RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new OAuth2AuthenticationException("invalid_client");
        }
        ClientSettings clientSettings = registeredClient.getClientSettings();
        String logoUri = clientSettings.getSetting(CustomClientSettings.LOGO_URI);
        String privacyUri = clientSettings.getSetting(CustomClientSettings.PRIVACY_POLICY_URI);
        String tosUri = clientSettings.getSetting(CustomClientSettings.TERMS_OF_SERVICE_URI);
        String appDomain = clientSettings.getSetting(CustomClientSettings.APP_DOMAIN);
        String developerEmail = clientSettings.getSetting(CustomClientSettings.DEVELOPER_EMAIL);
        String url = UriComponentsBuilder.fromUriString(securityProperties.getUiUrl())
                .path("/oauth2/consent")
                .queryParam("client_id", clientId)
                .queryParam("client_name", registeredClient.getClientName())
                .queryParam("logo_uri", logoUri)
                .queryParam("privacy_uri", privacyUri)
                .queryParam("terms_uri", tosUri)
                .queryParam("app_domain", appDomain)
                .queryParam("developer_email", developerEmail)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("type", type)
                .queryParam("user_code", userCode)
                .build()
                .encode()
                .toUriString();
        return "redirect:" + url;
    }

}
