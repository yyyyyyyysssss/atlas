package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.CreateUserSpec;
import com.atlas.common.core.api.user.dto.RoleAuthDTO;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserProfileDTO;
import com.atlas.common.core.response.Result;
import com.atlas.common.security.model.RequestUrlAuthority;
import com.atlas.common.security.model.SecurityUser;
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

    private final UserPasswordCredentialsService userPasswordCredentialsService;

    private final UserMfaService userMfaService;

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
            throw new AuthServiceException("加载用户认证信息失败: " + result.getMessage());
        }

        UserAuthDTO userAuthDTO = result.getData();
        SecurityUser securityUser = toSecurityUser(userAuthDTO);
        String username = userIdentifierService.findValueByUserIdAndType(userId, IdentifierType.USERNAME);
        securityUser.setUsername(username);

        // mfa
        UserMfaInfo userMfaInfo = userMfaService.getUserMfaInfo(userId);
        securityUser.setMfaEnabled(userMfaInfo.enabled());
        securityUser.setActiveMfaStrategies(userMfaInfo.enabledTypes());
        securityUser.setPreferredMfaType(userMfaInfo.preferredMfaType());

        return securityUser;
    }

    public UserProfile userProfile(Long userId){
        Result<UserProfileDTO> result = userApi.userProfile(userId);
        if(!result.isSucceed()){
            throw new AuthServiceException("加载用户认证信息失败: " + result.getMessage());
        }
        UserProfileDTO userProfileDTO = result.getData();
        List<UserIdentifier> userIdentifiers = userIdentifierService.listByUserId(userId);
        return UserProfile.of(userProfileDTO, userIdentifiers);
    }

    @Transactional
    public Long ensureUserByIdentifier(IdentifierType type, String value){
        Long userId = userIdentifierService.findUserIdByValueAndType(value, type);
        if(userId != null){
            return userId;
        }
        // 创建用户
        userId = invokeCreateUser(CreateUserSpec.empty());
        // 创建用户标识
        List<IdentifierSpec> specs = new ArrayList<>();
        if(!type.equals(IdentifierType.USERNAME)){
            specs.add(new IdentifierSpec(IdentifierType.USERNAME, null, null));
        }
        specs.add(new IdentifierSpec(type, value, true));
        userIdentifierService.addIdentifier(userId, specs);
        return userId;
    }

    @Transactional
    public Long ensureUserByProvider(String provider, ThirdPartyUserIdentity userIdentity){
        String sub = userIdentity.getSub();
        UserProviderDTO existingIdentity = userProviderService.getByProvider(provider, sub);
        if(existingIdentity != null){
            return existingIdentity.getUserId();
        }
        // 用三方带回的原生标识（邮箱/手机）去本地撞库，防止同人多号
        Long matchedEmailUserId = StringUtils.hasText(userIdentity.getEmail()) ? userIdentifierService.findUserIdByValueAndType(userIdentity.getEmail(), IdentifierType.EMAIL) : null;
        Long matchedPhoneUserId = StringUtils.hasText(userIdentity.getPhone()) ? userIdentifierService.findUserIdByValueAndType(userIdentity.getPhone(), IdentifierType.PHONE) : null;
        // 优先使用邮箱撞库命中的 ID，其次是手机
        Long userId = matchedEmailUserId != null ? matchedEmailUserId : matchedPhoneUserId;
        if (userId != null) {
            // 命中本地老账号！直接为老账号绑定该三方关系（静默绑定），不建新号
            userProviderService.addUserProvider(userId, provider,sub,userIdentity.getExtraInfo());
            List<IdentifierSpec> missingSpecs = new ArrayList<>();
            if(StringUtils.hasText(userIdentity.getEmail()) && matchedEmailUserId == null){
                missingSpecs.add(new IdentifierSpec(IdentifierType.EMAIL, userIdentity.getEmail(), userIdentity.getEmailVerified()));
            }
            if(StringUtils.hasText(userIdentity.getPhone()) && matchedPhoneUserId == null){
                missingSpecs.add(new IdentifierSpec(IdentifierType.PHONE, userIdentity.getPhone(), userIdentity.getPhoneVerified()));
            }
            userIdentifierService.addIdentifier(userId, missingSpecs);
            return userId;
        }
        // 创建用户
        userId = invokeCreateUser(new CreateUserSpec(userIdentity.getFullName(),userIdentity.getAvatar(),null));
        // 创建身份关联记录
        userProviderService.addUserProvider(userId, provider,sub,userIdentity.getExtraInfo());
        // 创建用户标识
        List<IdentifierSpec> specs = new ArrayList<>();
        specs.add(IdentifierSpec.generatedUsername());
        if(StringUtils.hasText(userIdentity.getEmail())){
            specs.add(IdentifierSpec.email(userIdentity.getEmail(), userIdentity.getEmailVerified()));
        }
        if(StringUtils.hasText(userIdentity.getPhone())){
            specs.add(IdentifierSpec.phone(userIdentity.getPhone(), userIdentity.getPhoneVerified()));
        }
        userIdentifierService.addIdentifier(userId, specs);
        return userId;
    }

    @Transactional
    public void bindThirdPartyProvider(String provider, Long currentUserId, ThirdPartyUserIdentity thirdPartyIdentity){
        String sub = thirdPartyIdentity.getSub();
        UserProviderDTO existingIdentity = userProviderService.getByProvider(provider, sub);
        if(existingIdentity != null){
            if(!existingIdentity.getUserId().equals(currentUserId)){
                throw new AuthServiceException("该社交账号已被其他用户绑定，请先解绑或更换账号");
            }
        }
        userProviderService.addUserProvider(currentUserId, provider,sub,thirdPartyIdentity.getExtraInfo());
    }

    private Long invokeCreateUser(CreateUserSpec spec) {
        Result<Long> result = userApi.createUser(spec);
        if (!result.isSucceed()) {
            throw new AuthServiceException("用户基础档案创建失败: " + result.getMessage());
        }
        if (result.getData() == null) {
            throw new AuthServiceException("用户基础档案创建失败: 远程服务未返回合法用户ID");
        }
        return result.getData();
    }

    private SecurityUser toSecurityUser(UserAuthDTO userAuthDTO){

        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(userAuthDTO.getId());

        String password = userPasswordCredentialsService.getPasswordHashByUserId(userAuthDTO.getId());

        securityUser.setPassword(password);

        // 补全 Spring Security 标准核心状态，防止框架拦截
        securityUser.setAccountNonExpired(true);
        securityUser.setAccountNonLocked(true);
        securityUser.setCredentialsNonExpired(true);

        securityUser.setFullName(userAuthDTO.getFullName());
        securityUser.setEnabled(userAuthDTO.isEnabled());
        securityUser.setDataScopes(userAuthDTO.getDataScopes());
        securityUser.setOrgId(userAuthDTO.getOrgId());
        List<RoleAuthDTO> authorities = userAuthDTO.getAuthorities();
        List<RequestUrlAuthority> authorityList = new ArrayList<>();
        for (RoleAuthDTO roleAuthDTO : authorities) {
            authorityList.add(new RequestUrlAuthority(roleAuthDTO.getCode(), roleAuthDTO.getAuthorityResources()));
        }
        securityUser.setAuthorities(authorityList);
        return securityUser;
    }
}
