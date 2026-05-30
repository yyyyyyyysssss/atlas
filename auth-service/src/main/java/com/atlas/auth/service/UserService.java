package com.atlas.auth.service;

import com.atlas.auth.domain.dto.IdentifierSpec;
import com.atlas.auth.domain.dto.OAuth2UserInfo;
import com.atlas.auth.domain.dto.UserProviderDTO;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.domain.entity.UserTotpCredentials;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.CreateUserSpec;
import com.atlas.common.core.api.user.dto.RoleAuthDTO;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
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

import java.util.*;
import java.util.stream.Collectors;

@Service("userService")
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserApi userApi;

    private final UserIdentifierService userIdentifierService;

    private final UserProviderService userProviderService;

    private final UserTotpCredentialsService userTotpCredentialsService;


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
        List<UserIdentifier> userIdentifiers = userIdentifierService.listByUserId(userId);
        UserAuthDTO userAuthDTO = result.getData();

        SecurityUser securityUser = securityUser(userAuthDTO, userIdentifiers);

        UserTotpCredentials userTotpCredentials = userTotpCredentialsService.getActivatedByUserId(userId);
        securityUser.setMfaEnabled(userTotpCredentials != null);
        return securityUser;
    }

    @Transactional
    public Long ensureUserByIdentifier(IdentifierType type, String value){
        Long userId = userIdentifierService.findUserIdByValueAndType(value, type);
        if(userId != null){
            return userId;
        }
        if(IdentifierType.USERNAME.equals(type)){
            throw new IllegalArgumentException("不支持对【用户名】类型执行自动创建用户操作");
        }
        // 创建用户
        userId = invokeCreateUser(CreateUserSpec.empty());
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
        Long matchedEmailUserId = StringUtils.hasText(extraInfo.getEmail()) ? userIdentifierService.findUserIdByValueAndType(extraInfo.getEmail(), IdentifierType.EMAIL) : null;
        Long matchedPhoneUserId = StringUtils.hasText(extraInfo.getPhone()) ? userIdentifierService.findUserIdByValueAndType(extraInfo.getPhone(), IdentifierType.PHONE) : null;
        // 优先使用邮箱撞库命中的 ID，其次是手机
        Long userId = matchedEmailUserId != null ? matchedEmailUserId : matchedPhoneUserId;
        if (userId != null) {
            // 命中本地老账号！直接为老账号绑定该三方关系（静默绑定），不建新号
            userProviderService.addUserProvider(userId, provider,sub,extraInfo.getExtraInfo());
            List<IdentifierSpec> missingSpecs = new ArrayList<>();
            if(StringUtils.hasText(extraInfo.getEmail()) && matchedEmailUserId == null){
                missingSpecs.add(new IdentifierSpec(IdentifierType.EMAIL, extraInfo.getEmail(), extraInfo.getEmailVerified()));
            }
            if(StringUtils.hasText(extraInfo.getPhone()) && matchedPhoneUserId == null){
                missingSpecs.add(new IdentifierSpec(IdentifierType.PHONE, extraInfo.getPhone(), extraInfo.getPhoneVerified()));
            }
            userIdentifierService.addIdentifier(userId, missingSpecs);
            return userId;
        }
        // 创建用户
        userId = invokeCreateUser(new CreateUserSpec(extraInfo.getFullName(),extraInfo.getAvatar(),null));
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

    private Long invokeCreateUser(CreateUserSpec spec) {
        Result<Long> result = userApi.createUser(spec);
        if (!result.isSucceed()) {
            throw new BusinessException("用户基础档案创建失败: " + result.getMessage());
        }
        if (result.getData() == null) {
            throw new BusinessException("用户基础档案创建失败: 远程服务未返回合法用户ID");
        }
        return result.getData();
    }

    public SecurityUser securityUser(UserAuthDTO userAuthDTO,List<UserIdentifier> userIdentifiers){
        Map<IdentifierType, String> identifierTypeMap = Optional.ofNullable(userIdentifiers)
                .orElse(Collections.emptyList())
                .stream()
                .filter(ui -> ui.getIdentifierType() != null)
                .collect(Collectors.toMap(
                        UserIdentifier::getIdentifierType,
                        ui -> ui.getIdentifierValue() == null ? "" : ui.getIdentifierValue(),
                        (oldVal, newVal) -> newVal
                ));
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(userAuthDTO.getId());
        securityUser.setUsername(identifierTypeMap.get(IdentifierType.USERNAME));
        securityUser.setEmail(identifierTypeMap.get(IdentifierType.EMAIL));
        securityUser.setPhone(identifierTypeMap.get(IdentifierType.PHONE));
        securityUser.setPassword(userAuthDTO.getPassword());

        // 补全 Spring Security 标准核心状态，防止框架拦截
        securityUser.setAccountNonExpired(true);
        securityUser.setAccountNonLocked(true);
        securityUser.setCredentialsNonExpired(true);

        securityUser.setFullName(userAuthDTO.getFullName());
        securityUser.setEnabled(userAuthDTO.isEnabled());
        securityUser.setDataScopes(userAuthDTO.getDataScopes());
        securityUser.setOrgId(userAuthDTO.getOrgId());
        securityUser.setAvatar(userAuthDTO.getAvatar());
        List<RoleAuthDTO> authorities = userAuthDTO.getAuthorities();
        List<RequestUrlAuthority> authorityList = new ArrayList<>();
        for (RoleAuthDTO roleAuthDTO : authorities) {
            authorityList.add(new RequestUrlAuthority(roleAuthDTO.getCode(), roleAuthDTO.getAuthorityUrls()));
        }
        securityUser.setAuthorities(authorityList);
        return securityUser;
    }
}
