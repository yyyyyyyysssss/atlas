package com.atlas.auth.service;

import com.atlas.auth.domain.dto.OAuth2UserInfo;
import com.atlas.auth.domain.dto.ThirdPartyLoginDTO;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.TokenResponse;
import jakarta.annotation.Resource;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/15 16:07
 */
public abstract class AbstractThirdPartyLoginProvider implements ThirdPartyLoginProvider {

    @Resource
    private LoginService loginService;

    @Resource
    private UserService userService;

    protected TokenResponse doLogin(OAuth2UserInfo oAuth2UserInfo){
        String provider = oAuth2UserInfo.getProvider();
        String sub = oAuth2UserInfo.getSub();
        Long userId = userService.ensureUserByProvider(provider, sub, oAuth2UserInfo);
        ThirdPartyLoginDTO thirdPartyLoginDTO = new ThirdPartyLoginDTO(ClientType.WEB, userId);
        return loginService.loginThirdParty(thirdPartyLoginDTO);
    }

}
