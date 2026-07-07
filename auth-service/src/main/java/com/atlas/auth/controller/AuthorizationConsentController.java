package com.atlas.auth.controller;

import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.atlas.auth.service.OAuth2ClientApplicationService;
import com.atlas.security.properties.SecurityProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
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

    @Resource
    private OAuth2ClientApplicationService oAuth2ClientApplicationService;

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
        // 获取该客户端的应用信息
        OAuth2ClientApplication auth2ClientApplication = oAuth2ClientApplicationService.getByRegisteredClientId(registeredClient.getId());
        // 构建url
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(securityProperties.getUiUrl())
                .path("/oauth2/consent")
                .queryParam("client_id", clientId)
                .queryParam("client_name", registeredClient.getClientName())
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("type", type);
        // 设备码授权独有的
        if(StringUtils.hasText(userCode)){
            builder.queryParam("user_code", userCode);
        }
        // 客户端应用信息
        if(auth2ClientApplication != null){
            builder
                    .queryParam("logo_uri", auth2ClientApplication.getLogoUrl())
                    .queryParam("privacy_uri",auth2ClientApplication.getPrivacyPolicyUrl())
                    .queryParam("terms_uri",auth2ClientApplication.getTermsServiceUrl())
                    .queryParam("home_page_url",auth2ClientApplication.getHomePageUrl())
                    .queryParam("developer_name",auth2ClientApplication.getDeveloperName())
                    .queryParam("developer_email",auth2ClientApplication.getDeveloperEmail());
        }

        String url = builder
                .build()
                .encode()
                .toUriString();
        return "redirect:" + url;
    }

}
