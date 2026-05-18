package com.atlas.auth.service;

import com.atlas.auth.domain.dto.IdentifierSpec;
import com.atlas.auth.domain.dto.UserProviderDTO;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.auth.service.impl.UserIdentifierServiceImpl;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.response.Result;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.token.ThirdPartyAuthenticationToken;
import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/15 16:07
 */
public abstract class AbstractThirdPartyLoginProvider implements ThirdPartyLoginProvider {

    @Resource
    private LoginService loginService;

    @Resource
    private UserProviderService userProviderService;

    @Resource
    private UserIdentifierService userIdentifierService;

    @Resource
    private UserApi userApi;

    protected TokenResponse doLogin(ExternalIdentityDTO externalIdentityDTO){
        String provider = externalIdentityDTO.getProvider();
        String sub = externalIdentityDTO.getSub();
        UserProviderDTO existingIdentity = userProviderService.getByProvider(provider, sub);
        Long userId;
        if (existingIdentity == null) {
            Result<UserDTO> userResult = userApi.ensureUser(externalIdentityDTO);
            if(!userResult.isSucceed()){
                throw new BusinessException("获取或注册用户失败: " + userResult.getMessage());
            }
            UserDTO userDTO = userResult.getData();
            userId = userDTO.getId();
            // 创建身份关联记录
            createUserProvider(userId, externalIdentityDTO);
            // 创建用户标识
            createUserIdentifier(userId,externalIdentityDTO);
        } else {
            userId = existingIdentity.getUserId();
        }
        ThirdPartyAuthenticationToken thirdPartyAuthenticationToken = new ThirdPartyAuthenticationToken(userId.toString(), null);
        return loginService.login(thirdPartyAuthenticationToken, ClientType.WEB, true, false);
    }

    public void createUserProvider(Long userId, ExternalIdentityDTO externalIdentityDTO){
        userProviderService.addUserProvider(userId, externalIdentityDTO.getProvider(),externalIdentityDTO.getSub(),externalIdentityDTO.getExtraInfo());
    }

    public void createUserIdentifier(Long userId, ExternalIdentityDTO externalIdentityDTO){
        List<IdentifierSpec> specs = new ArrayList<>();
        specs.add(new IdentifierSpec(IdentifierType.USERNAME, null, null));
        if(externalIdentityDTO.getEmail() != null){
            specs.add(new IdentifierSpec(IdentifierType.EMAIL, externalIdentityDTO.getEmail(), externalIdentityDTO.getEmailVerified()));
        }
        if(externalIdentityDTO.getPhone() != null){
            specs.add(new IdentifierSpec(IdentifierType.PHONE, externalIdentityDTO.getPhone(), externalIdentityDTO.getPhoneVerified()));
        }
        userIdentifierService.addIdentifier(userId, specs);
    }

}
