package com.atlas.auth.service;

import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.domain.entity.UserWebauthnCredentials;
import com.atlas.auth.domain.vo.*;
import com.atlas.auth.enums.CaptchaType;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.auth.enums.SecurityScene;
import com.atlas.auth.mapper.UserWebauthnCredentialsMapper;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.ServletHolder;
import com.atlas.common.redis.utils.RedisHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

    private final UserPasswordCredentialsService userPasswordCredentialsService;

    private final CaptchaFactory captchaFactory;

    private final RedisHelper redisHelper;

    private final UserWebauthnCredentialsMapper userWebauthnCredentialsMapper;

    private final WebauthnService webauthnService;

    private final UserCredentialRepository userCredentialRepository;


    public AccountSecurityVO getAccountSecurity(Long userId) {
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

        // 通行密钥
        List<UserPasskeyVO> passkeyVOs = Collections.emptyList();
        List<UserWebauthnCredentials> userPasswordCredentials = userWebauthnCredentialsMapper.selectList(
                new LambdaQueryWrapper<UserWebauthnCredentials>()
                        .eq(UserWebauthnCredentials::getUserId, userId)
        );
        if (!CollectionUtils.isEmpty(userPasswordCredentials)) {
            passkeyVOs = userPasswordCredentials.stream()
                    .map(crypto -> UserPasskeyVO.builder()
                            .credentialId(crypto.getCredentialId())
                            .userId(crypto.getUserId())
                            .label(crypto.getLabel())
                            .createTime(crypto.getCreateTime())
                            .build())
                    .toList();
        }

        return AccountSecurityVO.builder()
                // 密码和密钥（暂不处理）
                .passwordSet(passwordSet)

                // 通行密钥
                .passkeys(passkeyVOs)
                .passkeyEnabled(!passkeyVOs.isEmpty())

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

    public void changeUsername(Long userId, ChangeUsernameDTO changeUsernameDTO) {
        UserIdentifier userIdentifier = userIdentifierService.findByUserIdAndType(userId, IdentifierType.USERNAME);
        if (isUsernameModified(userIdentifier)) {
            throw new BusinessException("账号名一年内仅允许修改一次，上次修改时间：" + userIdentifier.getUpdateTime().toLocalDate());
        }
        String newUsername = changeUsernameDTO.newUsername();
        Long existsUserId = userIdentifierService.findUserIdByValueAndType(newUsername, IdentifierType.USERNAME);
        if (existsUserId != null) {
            throw new BusinessException("该账号名已被占用，请换一个试试");
        }
        userIdentifierService.updateIdentifier(userId, IdentifierType.USERNAME, newUsername, true);
    }

    public void initPassword(Long userId, InitPasswordDTO initPasswordDTO) {
        if (!Objects.equals(initPasswordDTO.password(), initPasswordDTO.confirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        userPasswordCredentialsService.setPassword(userId, initPasswordDTO.password());
    }

    public void changePassword(Long userId, ChangePasswordDTO changePasswordDTO) {
        if (!Objects.equals(changePasswordDTO.newPassword(), changePasswordDTO.confirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        boolean verify = userPasswordCredentialsService.verifyPassword(userId, changePasswordDTO.newPassword());
        if (verify) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        // 所有外部校验全部通过后，最后核验并销毁第一步的凭证
        validTicket(userId, SecurityScene.RESET_PASSWORD, changePasswordDTO.ticket());
        userPasswordCredentialsService.updatePassword(userId, changePasswordDTO.newPassword());
    }

    public void initEmail(Long userId, InitEmailDTO initEmailDTO) {
        Long exist = userIdentifierService.findUserIdByValueAndType(initEmailDTO.email(), IdentifierType.EMAIL);
        if (exist != null) {
            throw new BusinessException("该邮箱已被其他账号占用");
        }
        boolean verify = captchaFactory.getService(CaptchaType.EMAIL)
                .verify(initEmailDTO.email(), initEmailDTO.code(), SecurityScene.MODIFY_EMAIL);
        if (!verify) {
            throw new BusinessException("验证码错误或已过期");
        }
        userIdentifierService.addIdentifier(userId, new IdentifierSpec(IdentifierType.EMAIL, initEmailDTO.email(), true));
    }

    public void changeEmail(Long userId, ChangeEmailDTO changeEmailDTO) {
        Long exist = userIdentifierService.findUserIdByValueAndType(changeEmailDTO.newEmail(), IdentifierType.EMAIL);
        if (exist != null) {
            throw new BusinessException("该邮箱已被其他账号占用");
        }
        boolean verify = captchaFactory.getService(CaptchaType.EMAIL)
                .verify(changeEmailDTO.newEmail(), changeEmailDTO.code(), SecurityScene.MODIFY_EMAIL);
        if (!verify) {
            throw new BusinessException("验证码错误或已过期");
        }
        // 所有外部校验全部通过后，最后核验并销毁第一步的凭证
        validTicket(userId, SecurityScene.MODIFY_EMAIL, changeEmailDTO.ticket());
        userIdentifierService.updateIdentifier(userId, IdentifierType.EMAIL, changeEmailDTO.newEmail(), true);
    }

    public VerifyPasswordVO verifyPassword(Long userId, VerifyPasswordDTO verifyPasswordDTO) {
        boolean verified = userPasswordCredentialsService.verifyPassword(userId, verifyPasswordDTO.password());
        String ticket = null;
        if (verified) {
            ticket = generateTicket(userId, verifyPasswordDTO.securityScene());
        }
        return new VerifyPasswordVO(verified, ticket);
    }

    public VerifyCaptchaVO verifyCaptcha(Long userId, CaptchaVerifyDTO captchaVerifyDTO) {
        boolean verified = captchaFactory.getService(captchaVerifyDTO.captchaType())
                .verify(captchaVerifyDTO.target(), captchaVerifyDTO.code(), captchaVerifyDTO.securityScene());
        String ticket = null;
        if (verified) {
            ticket = generateTicket(userId, captchaVerifyDTO.securityScene());
        }
        return new VerifyCaptchaVO(verified, ticket);
    }

    public VerifyWebauthnVO verifyWebauthn(Long userId,WebauthnPublicKeyCredentialRequest webauthnPublicKeyCredentialRequest, SecurityScene securityScene) {
        boolean verified = false;
        try {
            WebauthnAuthenticateResponse webauthnAuthenticateResponse = webauthnService.authenticate(webauthnPublicKeyCredentialRequest);
            String credentialId = webauthnAuthenticateResponse.credentialId();
            CredentialRecord credentialRecord = userCredentialRepository.findByCredentialId(Bytes.fromBase64(credentialId));
            if(credentialRecord != null){
                Bytes userEntityUserId = credentialRecord.getUserEntityUserId();
                long expectedUserId = Long.parseLong(new String(userEntityUserId.getBytes(), StandardCharsets.UTF_8));
                if(userId.equals(expectedUserId)){
                    verified = true;
                }
            }
        }catch (Exception e){
            log.error("webauthn authenticate error", e);
            verified = false;
        }
        String ticket = null;
        if (verified) {
            ticket = generateTicket(userId, securityScene);
        }
        return new VerifyWebauthnVO(verified, ticket);
    }

    public void unbindWebauthn(Long userId,UnbindWebauthnDTO unbindWebauthnDTO){
        // 解码凭证 ID
        Bytes credentialId;
        try {
            credentialId = Bytes.fromBase64(unbindWebauthnDTO.credentialId());
        } catch (Exception e) {
            log.warn("用户 {} 尝试使用非法的 Base64 凭证 ID 进行解绑", userId);
            throw new BusinessException("设备凭证无效");
        }
        // 查找凭证
        CredentialRecord credentialRecord = userCredentialRepository.findByCredentialId(credentialId);
        if (credentialRecord == null) {
            throw new BusinessException("设备凭证不存在");
        }

        Bytes userEntityUserId = credentialRecord.getUserEntityUserId();
        long expectedUserId;
        try {
            expectedUserId = Long.parseLong(new String(userEntityUserId.getBytes(), StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            log.error("WebAuthn 凭证用户ID解析失败，原始字节转字符串为: {}", new String(userEntityUserId.getBytes()));
            throw new BusinessException("凭证用户数据不匹配");
        }
        // 越权校验
        if(!userId.equals(expectedUserId)){
            log.warn("用户 {} 尝试越权解绑用户 {} 的凭证", userId, expectedUserId);
            throw new BusinessException("设备凭证不存在"); // 防枚举，保持模糊提示
        }
        // 风控验证码/Ticket 校验
        validTicket(userId, SecurityScene.UNBIND_WEBAUTHN, unbindWebauthnDTO.ticket());
        userCredentialRepository.delete(credentialId);
        log.info("用户 {} 成功解绑 WebAuthn 凭证: {}", userId, unbindWebauthnDTO.credentialId());
    }

    private String generateTicket(Long userId, SecurityScene securityScene) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        String redisKey = "account:ticket:" + securityScene.getCode() + ":" + ticket;
        redisHelper.setValue(redisKey, userId, Duration.ofMinutes(5));
        return ticket;
    }

    private Long validTicket(Long userId, SecurityScene securityScene, String ticket) {
        String redisKey = "account:ticket:" + securityScene.getCode() + ":" + ticket;
        Long ticketUserId = redisHelper.getValue(redisKey, Long.class);
        if (ticketUserId == null) {
            throw new BusinessException("安全验证已过期，请重新进行身份验证");
        }
        if (!ticketUserId.equals(userId)) {
            throw new BusinessException("安全校验未通过，请重新进行身份验证");
        }
        redisHelper.delete(redisKey);
        return ticketUserId;
    }


    private boolean isUsernameModified(UserIdentifier userIdentifier) {

        return userIdentifier.getUpdateTime() != null && userIdentifier.getUpdateTime().isAfter(userIdentifier.getCreateTime());
    }

}
