package com.atlas.auth.config.security.saml2;

import com.atlas.auth.domain.dto.Saml2UserInfo;
import com.atlas.auth.domain.dto.ThirdPartyLoginDTO;
import com.atlas.auth.service.LoginService;
import com.atlas.auth.service.UserService;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.properties.SecurityProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:25
 */
@Component
@Slf4j
public class Saml2SuccessHandler implements AuthenticationSuccessHandler {


    private final SecurityProperties securityProperties;

    private final UserService userService;

    private final LoginService loginService;

    public Saml2SuccessHandler(SecurityProperties securityProperties,UserService userService, LoginService loginService){
        this.securityProperties = securityProperties;
        this.userService = userService;
        this.loginService = loginService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
        String registrationId = principal.getRelyingPartyRegistrationId();
        Saml2UserInfo saml2User = extractUserByProvider(principal, registrationId);
        log.info("SAML2 认证成功! 来源 IdP: {}, 用户数据: {}", registrationId, saml2User);

        Long userId = userService.ensureUserByProvider(registrationId, saml2User);

        ThirdPartyLoginDTO thirdPartyLoginDTO = new ThirdPartyLoginDTO(ClientType.WEB, userId);
        TokenResponse tokenResponse = loginService.loginThirdParty(thirdPartyLoginDTO);

        String uiUrl = securityProperties.getUiUrl();
        String targetUrl = String.format("%s/login/saml2/callback?accessToken=%s&refreshToken=%s",
                uiUrl, tokenResponse.token().access().value(), tokenResponse.token().refresh().value());
        response.sendRedirect(targetUrl);
    }

    private Saml2UserInfo extractUserByProvider(Saml2AuthenticatedPrincipal principal, String provider) {
        Saml2UserInfo saml2User = new Saml2UserInfo();
        saml2User.setSub(principal.getName());
        switch (provider.toLowerCase()) {
            case "auth0":
                String email = principal.getFirstAttribute("http://schemas.auth0.com/email");
                Object verifiedObj = principal.getFirstAttribute("http://schemas.auth0.com/email_verified");
                boolean emailVerified = verifiedObj != null && Boolean.parseBoolean(verifiedObj.toString());
                String fullName = principal.getFirstAttribute("http://schemas.auth0.com/name");
                String avatar = principal.getFirstAttribute("http://schemas.auth0.com/picture");
                saml2User.setEmail(email);
                saml2User.setEmailVerified(emailVerified);
                saml2User.setFullName(fullName);
                saml2User.setAvatar(avatar);
                break;
            default:
                // 兜底：尝试直接读取标准短 Key
                saml2User.setEmail(principal.getFirstAttribute("email"));
                saml2User.setFullName(principal.getFirstAttribute("name"));
                saml2User.setAvatar(principal.getFirstAttribute("avatar"));
        }
        Map<String, Object> extraInfo = JsonUtils.convert(principal, new TypeReference<>() {});
        saml2User.setExtraInfo(extraInfo);
        return saml2User;
    }
}
