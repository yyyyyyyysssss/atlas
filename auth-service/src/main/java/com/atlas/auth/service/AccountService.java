package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ChangeUsernameDTO;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.domain.entity.UserProvider;
import com.atlas.auth.domain.vo.AccountSecurityVO;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        Set<String> boundProviderNames = userProviders.stream()
                .map(UserProvider::getProvider)
                .filter(Objects::nonNull) // 顺手做个防呆过滤，防止数据库里有空字段导致空指针
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

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
                .googleBound(boundProviderNames.contains("GOOGLE"))
                .githubBound(boundProviderNames.contains("GITHUB"))

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
