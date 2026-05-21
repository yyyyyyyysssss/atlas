package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ChangeUsernameDTO;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.domain.entity.UserProvider;
import com.atlas.auth.domain.vo.AccountSecurityVO;
import com.atlas.auth.domain.vo.UserProviderVO;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.auth.enums.ProviderType;
import com.atlas.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/21 11:55
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserIdentifierService userIdentifierService;

    private final UserProviderService userProviderService;

    public AccountSecurityVO getAccountSecurity(Long userId){
        List<UserIdentifier> userIdentifiers = userIdentifierService.listByUserId(userId);
        Map<IdentifierType, UserIdentifier> identifierMap = userIdentifiers.stream()
                .collect(Collectors.toMap(
                        UserIdentifier::getIdentifierType,
                        item -> item,
                        (k1, k2) -> k1
                ));
        UserIdentifier usernameIdent = identifierMap.get(IdentifierType.USERNAME);
        UserIdentifier emailIdent = identifierMap.get(IdentifierType.EMAIL);
        UserIdentifier phoneIdent = identifierMap.get(IdentifierType.PHONE);

        // 三方账号
        List<UserProvider> userProviders = userProviderService.listByUserId(userId);
        Map<String, UserProvider> providerMap = userProviders.stream()
                .filter(item -> item.getProvider() != null)
                .collect(Collectors.toMap(
                        item -> item.getProvider().toUpperCase(),
                        item -> item,
                        (k1, k2) -> k1
                ));
        List<UserProviderVO> providers = Arrays.stream(ProviderType.values())
                .map(supported -> {
                    String code = supported.getCode();
                    boolean isBound = providerMap.containsKey(code);
                    String boundName = null;
                    if (isBound) {
                        UserProvider providerData = providerMap.get(code);
                        Map<String, Object> extraInfo = providerData.getExtraInfo();
                        boundName = "已关联账户";
                        if (extraInfo != null && !extraInfo.isEmpty()) {
                            if (ProviderType.GOOGLE.getCode().equals(code)) {
                                // Google 授权通常返回 email
                                Object emailObj = extraInfo.get("email");
                                if (emailObj != null) {
                                    boundName = emailObj.toString();
                                }
                            } else if (ProviderType.GITHUB.getCode().equals(code)) {
                                // GitHub 标准字段是 login (用户名)
                                Object loginObj = extraInfo.get("login");
                                if (loginObj != null) {
                                    boundName = loginObj.toString();
                                }
                            } else if (ProviderType.ATLAS.getCode().equals(code)) {
                                // Atlas通常是 preferred_username
                                Object preferredUsername = extraInfo.get("preferred_username");
                                if (preferredUsername != null) {
                                    boundName = preferredUsername.toString();
                                }
                            }
                        }
                    }

                    return UserProviderVO.builder()
                            .provider(code.toLowerCase()) // 转小写给前端当 key (如 "google")
                            .isBound(isBound)
                            .boundName(boundName)
                            .build();
                })
                .toList();

        return AccountSecurityVO.builder()
                // 密码和密钥（暂不处理）
                .passwordSet(true)
                .passkeyBound(false)

                // 来自 user_identifier 表 的基础通信资产
                .username(usernameIdent != null ? usernameIdent.getIdentifierValue() : null)
                .isUsernameModified(usernameIdent != null && isUsernameModified(usernameIdent))
                .boundEmail(emailIdent != null ? emailIdent.getIdentifierValue() : null)
                .emailVerified(emailIdent != null && emailIdent.getVerified())
                .boundPhone(phoneIdent != null ? phoneIdent.getIdentifierValue() : null)
                .phoneVerified(phoneIdent != null && phoneIdent.getVerified())

                // 来自 user_provider 表 的社交绑定资产
                .providers(providers)

                // 2FA 两步验证状态（暂不处理）
                .mfaEnabled(false)
                .recoveryCodeGenerated(false)
                .build();
    }

    public void changeUsername(Long userId, ChangeUsernameDTO changeUsernameDTO){
        UserIdentifier userIdentifier = userIdentifierService.findByUserIdAndType(userId, IdentifierType.USERNAME);
        if (isUsernameModified(userIdentifier)) {
            throw new BusinessException("账号名一年内仅允许修改一次，上次修改时间：" + userIdentifier.getUpdateTime().toLocalDate());
        }
        String newUsername = changeUsernameDTO.newUsername();
        Long existsUserId = userIdentifierService.findUserIdByValueAndType(newUsername, IdentifierType.USERNAME);
        if (existsUserId != null) {
            throw new BusinessException("该账号名已被占用，请换一个试试");
        }
        userIdentifierService.updateUsername(userId, newUsername);
    }

    private boolean isUsernameModified(UserIdentifier userIdentifier){

        return userIdentifier.getUpdateTime() != null && userIdentifier.getUpdateTime().isAfter(userIdentifier.getCreateTime());
    }

}
