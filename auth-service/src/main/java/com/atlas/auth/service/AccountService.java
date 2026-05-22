package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ChangePasswordDTO;
import com.atlas.auth.domain.dto.ChangeUsernameDTO;
import com.atlas.auth.domain.dto.InitPasswordDTO;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.domain.vo.AccountSecurityVO;
import com.atlas.auth.domain.vo.UserProviderVO;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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

    private final UserPasswordCredentialsService userPasswordCredentialsService;

    public AccountSecurityVO getAccountSecurity(Long userId){
        // 账号标识
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
        List<UserProviderVO> providers = userProviderService.getUserProviderViewList(userId);

        // 密码
        boolean passwordSet = userPasswordCredentialsService.hasPassword(userId);

        return AccountSecurityVO.builder()
                // 密码和密钥（暂不处理）
                .passwordSet(passwordSet)
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

    public void initPassword(Long userId, InitPasswordDTO initPasswordDTO){
        userPasswordCredentialsService.setPassword(userId,initPasswordDTO.password());
    }

    public void changePassword(Long userId, ChangePasswordDTO changePasswordDTO){
        userPasswordCredentialsService.updatePassword(userId,changePasswordDTO.oldPassword(),changePasswordDTO.newPassword());
    }

    private boolean isUsernameModified(UserIdentifier userIdentifier){

        return userIdentifier.getUpdateTime() != null && userIdentifier.getUpdateTime().isAfter(userIdentifier.getCreateTime());
    }

}
