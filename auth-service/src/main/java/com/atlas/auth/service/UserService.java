package com.atlas.auth.service;

import com.atlas.auth.domain.dto.OAuth2UserInfo;
import com.atlas.auth.domain.dto.IdentifierSpec;
import com.atlas.auth.domain.dto.UserProviderDTO;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.*;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.response.Result;
import com.atlas.security.model.RequestUrlAuthority;
import com.atlas.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service("userService")
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserApi userApi;

    private final UserIdentifierService userIdentifierService;

    private final UserProviderService userProviderService;

    public UserDTO findByUserId(Long userId){
        Result<UserDTO> result = userApi.findByUserId(userId);
        if(!result.isSucceed()){
            return null;
        }
        return result.getData();
    }

    @Transactional
    public Long ensureUserByIdentifier(IdentifierType type, String value){
        Long userId = userIdentifierService.findUserIdByValueAndType(value, type);
        if(userId != null){
            return userId;
        }
        // 创建用户
        Result<Long> result = userApi.createUser(CreateUserSpec.empty());
        if(!result.isSucceed()){
            throw new BusinessException("用户创建失败: " + result.getMessage());
        }
        userId = result.getData();
        // 创建用户标识
        List<IdentifierSpec> specs = new ArrayList<>();
        specs.add(new IdentifierSpec(IdentifierType.USERNAME, null, null));
        specs.add(new IdentifierSpec(type, value, true));
        userIdentifierService.addIdentifier(userId, specs);
        return userId;
    }

    @Transactional
    public Long ensureUserByProvider(String provider, String sub, OAuth2UserInfo extraInfo){
        UserProviderDTO existingIdentity = userProviderService.getByProvider(provider, sub);
        if(existingIdentity != null){
            return existingIdentity.getUserId();
        }
        // 用三方带回的原生标识（邮箱/手机）去本地撞库，防止同人多号
        Long userId = null;
        if(StringUtils.hasText(extraInfo.getEmail())){
            userId = userIdentifierService.findUserIdByValueAndType(extraInfo.getEmail(), IdentifierType.EMAIL);
        }
        if(userId == null && StringUtils.hasText(extraInfo.getPhone())){
            userId = userIdentifierService.findUserIdByValueAndType(extraInfo.getPhone(), IdentifierType.PHONE);
        }
        if (userId != null) {
            // 命中本地老账号！直接为老账号绑定该三方关系（静默绑定），不建新号
            userProviderService.addUserProvider(userId, provider,sub,extraInfo.getExtraInfo());
            List<IdentifierSpec> missingSpecs = new ArrayList<>();
            if(StringUtils.hasText(extraInfo.getEmail())){
                missingSpecs.add(new IdentifierSpec(IdentifierType.EMAIL, extraInfo.getEmail(), extraInfo.getEmailVerified()));
            }
            if(StringUtils.hasText(extraInfo.getPhone())){
                missingSpecs.add(new IdentifierSpec(IdentifierType.PHONE, extraInfo.getPhone(), extraInfo.getPhoneVerified()));
            }
            userIdentifierService.addIdentifier(userId, missingSpecs);
            return userId;
        }
        // 创建用户
        Result<Long> result = userApi.createUser(new CreateUserSpec(extraInfo.getFullName(),extraInfo.getAvatar(),null));
        if(!result.isSucceed()){
            throw new BusinessException("用户创建失败: " + result.getMessage());
        }
        userId = result.getData();
        // 创建身份关联记录
        userProviderService.addUserProvider(userId, provider,sub,extraInfo.getExtraInfo());
        // 创建用户标识
        List<IdentifierSpec> specs = new ArrayList<>();
        specs.add(new IdentifierSpec(IdentifierType.USERNAME, null, null));
        if(StringUtils.hasText(extraInfo.getEmail())){
            specs.add(new IdentifierSpec(IdentifierType.EMAIL, extraInfo.getEmail(), extraInfo.getEmailVerified()));
        }
        if(StringUtils.hasText(extraInfo.getPhone())){
            specs.add(new IdentifierSpec(IdentifierType.PHONE, extraInfo.getPhone(), extraInfo.getPhoneVerified()));
        }
        userIdentifierService.addIdentifier(userId, specs);
        return userId;
    }



    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserIdentifier userIdentifier = userIdentifierService.findByValue(username);
        if(userIdentifier == null){
            throw new UsernameNotFoundException("user not fund");
        }
        return loadUserByUserId(userIdentifier.getUserId());
    }

    public UserDetails loadUserByUserId(Long userId) throws UsernameNotFoundException{
        Result<UserAuthDTO> result = userApi.loadUserByUserId(userId);
        if(!result.isSucceed()){
            throw new UsernameNotFoundException(result.getMessage());
        }
        UserAuthDTO userAuthDTO = result.getData();
        return securityUser(userAuthDTO);
    }


    public SecurityUser securityUser(UserAuthDTO userAuthDTO){
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(userAuthDTO.getId());
        securityUser.setUsername(userAuthDTO.getUsername());
        securityUser.setPassword(userAuthDTO.getPassword());
        securityUser.setFullName(userAuthDTO.getFullName());
        securityUser.setEnabled(userAuthDTO.isEnabled());
        securityUser.setDataScopes(userAuthDTO.getDataScopes());
        securityUser.setOrgId(userAuthDTO.getOrgId());
        securityUser.setAvatar(userAuthDTO.getAvatar());
        securityUser.setEmail(userAuthDTO.getEmail());
        securityUser.setPhone(userAuthDTO.getPhone());
        List<RoleAuthDTO> authorities = userAuthDTO.getAuthorities();
        List<RequestUrlAuthority> authorityList = new ArrayList<>();
        for (RoleAuthDTO roleAuthDTO : authorities) {
            authorityList.add(new RequestUrlAuthority(roleAuthDTO.getCode(), roleAuthDTO.getAuthorityUrls()));
        }
        securityUser.setAuthorities(authorityList);
        return securityUser;
    }
}
