package com.atlas.auth.service;

import com.atlas.auth.domain.dto.IdentifierSpec;
import com.atlas.auth.domain.dto.ThirdPartyUserIdentity;
import com.atlas.auth.domain.dto.UserProviderDTO;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.CreateUserSpec;
import com.atlas.common.core.api.user.dto.RoleAuthDTO;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.response.Result;
import com.atlas.security.model.MfaType;
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

    private final UserGestureCredentialsService userGestureCredentialsService;

    private final UserMfaBackupCodeService userMfaBackupCodeService;

    private final UserPasswordCredentialsService userPasswordCredentialsService;

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

        // 织入多因子认证（MFA）
        populateUserMfa(securityUser, userId);

        return securityUser;
    }

    private void populateUserMfa(SecurityUser securityUser, Long userId) {
        boolean hasTotp = userTotpCredentialsService.getActivatedByUserId(userId) != null;
        boolean hasGesture = userGestureCredentialsService.getByUserId(userId).isPresent();
        boolean hasBackupCode = userMfaBackupCodeService.hasActiveCodes(userId);

        boolean mfaEnabled = hasTotp || hasGesture;
        securityUser.setMfaEnabled(mfaEnabled);
        if (mfaEnabled) {
            Set<MfaType> strategies = new HashSet<>();
            if (hasTotp){
                strategies.add(MfaType.TOTP);
            }
            if (hasGesture) {
                strategies.add(MfaType.GESTURE);
            }
            if (hasBackupCode){
                strategies.add(MfaType.BACKUP_CODE);
            }
            securityUser.setActiveMfaStrategies(strategies);
            if (hasTotp) {
                securityUser.setPreferredMfaType(MfaType.TOTP);
            } else {
                securityUser.setPreferredMfaType(MfaType.GESTURE);
            }
        } else {
            securityUser.setActiveMfaStrategies(Collections.emptySet());
            securityUser.setPreferredMfaType(null);
        }

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
        specs.add(new IdentifierSpec(IdentifierType.USERNAME, null, null));
        if(StringUtils.hasText(userIdentity.getEmail())){
            specs.add(new IdentifierSpec(IdentifierType.EMAIL, userIdentity.getEmail(), userIdentity.getEmailVerified()));
        }
        if(StringUtils.hasText(userIdentity.getPhone())){
            specs.add(new IdentifierSpec(IdentifierType.PHONE, userIdentity.getPhone(), userIdentity.getPhoneVerified()));
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
                throw new BusinessException("该社交账号已被其他用户绑定，请先解绑或更换账号");
            }
        }
        userProviderService.addUserProvider(currentUserId, provider,sub,thirdPartyIdentity.getExtraInfo());
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
