package com.atlas.auth.service;

import com.atlas.auth.domain.dto.UserIdentityDTO;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.response.Result;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.token.ThirdPartyAuthenticationToken;
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
    private UserIdentityService userIdentityService;

    @Resource
    private UserApi userApi;

    protected TokenResponse doLogin(ExternalIdentityDTO externalIdentityDTO){
        String provider = externalIdentityDTO.getProvider();
        String sub = externalIdentityDTO.getSub();
        UserIdentityDTO existingIdentity = userIdentityService.getByIdentity(provider, sub);
        String username;
        if (existingIdentity == null) {
            Result<UserDTO> userResult = userApi.ensureUser(externalIdentityDTO);
            if(!userResult.isSucceed()){
                throw new BusinessException("获取或注册用户失败: " + userResult.getMessage());
            }
            UserDTO userDTO = userResult.getData();
            // 创建身份关联记录
            userIdentityService.addUserIdentity(userDTO.getId(), externalIdentityDTO);
            username = userDTO.getUsername();
        } else {
            username = existingIdentity.getIdentifier();
        }
        ThirdPartyAuthenticationToken thirdPartyAuthenticationToken = new ThirdPartyAuthenticationToken(username, null);
        return loginService.login(thirdPartyAuthenticationToken, ClientType.WEB, true, false);
    }

}
