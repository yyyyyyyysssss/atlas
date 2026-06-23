package com.atlas.auth.service;

import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.entity.*;
import com.atlas.auth.domain.vo.*;
import com.atlas.auth.enums.*;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.token.WebauthnAuthenticationRequest;
import com.atlas.security.utils.TicketGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final WebauthnService webauthnService;

    private final UserWebauthnCredentialsService userWebauthnCredentialsService;

    private final TotpService totpService;

    private final UserTotpCredentialsService userTotpCredentialsService;

    private final UserMfaBackupCodeService userMfaBackupCodeService;

    private final UserGestureCredentialsService userGestureCredentialsService;

    private final UserWeb3CredentialsService userWeb3CredentialsService;

    private final Web3WalletService web3WalletService;

    private final ThirdPartyLoginProviderFactory thirdPartyLoginProviderFactory;

    private final List<AuthCredentialChecker> credentialCheckers;

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
        List<UserWebauthnCredentials> userPasswordCredentials = userWebauthnCredentialsService.listByUserId(userId);

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

        // 手势凭证
        UserGestureCredentials userGestureCredentials = userGestureCredentialsService.getByUserId(userId).orElse(null);

        // totp
        UserTotpCredentials userTotpCredentials = userTotpCredentialsService.getByUserId(userId);
        // 剩余备份码数量
        int remainingBackupCodeCount = userMfaBackupCodeService.countRemainingCodes(userId);

        // web3.0凭证
        List<UserWeb3WalletVO> web3Wallets = Collections.emptyList();
        List<UserWeb3Credentials> userWeb3Credentials = userWeb3CredentialsService.listByUserId(userId);
        if (!CollectionUtils.isEmpty(userWeb3Credentials)) {
            web3Wallets = userWeb3Credentials.stream()
                    .map(crypto -> UserWeb3WalletVO.builder()
                            .id(crypto.getId())
                            .userId(crypto.getUserId())
                            .address(crypto.getAddress())
                            .walletType(crypto.getWalletType())
                            .source(crypto.getSource())
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

                // 手势凭证
                .gestureEnabled(userGestureCredentials != null)

                // web3凭证
                .web3Enabled(!web3Wallets.isEmpty())
                .web3Wallets(web3Wallets)

                // 来自 user_identifier 表 的基础通信资产
                .username(usernameIdent != null ? usernameIdent.getIdentifierValue() : null)
                .isUsernameModified(usernameIdent != null && isUsernameModified(usernameIdent))
                .boundEmail(emailIdent != null ? emailIdent.getIdentifierValue() : null)
                .emailVerified(emailIdent != null && emailIdent.getVerified())
                .boundPhone(phoneIdent != null ? phoneIdent.getIdentifierValue() : null)
                .phoneVerified(phoneIdent != null && phoneIdent.getVerified())

                // 来自 user_provider 表 的社交绑定资产
                .providers(providers)

                // 2FA 两步验证状态
                .totpEnabled(userTotpCredentials != null && userTotpCredentials.getStatus().equals(UserTotpStatus.ACTIVATED))
                .backupCodeGenerated(remainingBackupCodeCount > 0)
                .remainingBackupCodeCount(remainingBackupCodeCount)
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
        validTicket(userId, SecurityScene.MODIFY_EMAIL, initEmailDTO.ticket());
        boolean verify = captchaFactory.getService(CaptchaType.EMAIL)
                .verify(initEmailDTO.email(), initEmailDTO.code(), CaptchaScene.MODIFY_EMAIL);
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
                .verify(changeEmailDTO.newEmail(), changeEmailDTO.code(), CaptchaScene.MODIFY_EMAIL);
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
                .verify(captchaVerifyDTO.target(), captchaVerifyDTO.code(), captchaVerifyDTO.captchaScene());
        String ticket = null;
        if (verified) {
            ticket = generateTicket(userId, captchaVerifyDTO.securityScene());
        }
        return new VerifyCaptchaVO(verified, ticket);
    }

    public VerifyWebauthnVO verifyWebauthn(Long userId, WebauthnAuthenticationRequest webauthnAuthenticationRequest, SecurityScene securityScene) {
        boolean verified = false;
        try {
            WebauthnAuthenticateResponse webauthnAuthenticateResponse = webauthnService.authenticate(webauthnAuthenticationRequest);
            Long expectedUserId = webauthnAuthenticateResponse.userId();
            if(userId.equals(expectedUserId)){
                verified = true;
            }
        }catch (Exception e){
            log.error("webauthn authenticate error", e);
        }
        String ticket = null;
        if (verified) {
            ticket = generateTicket(userId, securityScene);
        }
        return new VerifyWebauthnVO(verified, ticket);
    }

    public WebauthnRegistrationResponse registerWebauthn(HttpServletRequest request, Long userId, WebAuthnRegistrationRequest webAuthnRegistrationRequest){
        Objects.requireNonNull(webAuthnRegistrationRequest.ticket(),"安全验证凭证缺失，请重新进行身份验证");
        validTicket(userId,SecurityScene.BIND_WEBAUTHN,webAuthnRegistrationRequest.ticket());
        return webauthnService.registerCredential(request, webAuthnRegistrationRequest.publicKey());
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
        CredentialRecord credentialRecord = userWebauthnCredentialsService.findByCredentialId(credentialId);
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
        // 防止孤儿账号
        ensureIdentityNotOrphaned(userId, CredentialType.WEBAUTHN, unbindWebauthnDTO.credentialId());
        // 解绑
        userWebauthnCredentialsService.delete(credentialId);
        log.info("用户 {} 成功解绑 WebAuthn 凭证: {}", userId, unbindWebauthnDTO.credentialId());
    }

    public TotpVerifyVO verifyTotp(Long userId, TotpVerifyDTO totpVerifyDTO){
        UserTotpCredentials credential = userTotpCredentialsService.getByUserId(userId);
        if (credential == null || !UserTotpStatus.ACTIVATED.equals(credential.getStatus())) {
            log.warn("用户 {} 尝试校验未激活或不存在的 TOTP 资源", userId);
            throw new BusinessException("当前账户尚未开启双因子认证保护");
        }
        boolean verified = false;
        boolean isMatched = totpService.verify(credential.getSecretKey(), totpVerifyDTO.code());
        if(isMatched){
            verified = true;
        }
        String ticket = null;
        if (verified) {
            ticket = generateTicket(userId, totpVerifyDTO.securityScene());
        }
        return new TotpVerifyVO(verified, ticket);
    }

    public TotpInitVO initTotp(Long userId,TotpInitDTO totpInitDTO){
        log.info("用户 {} 开始申请绑定/更换 TOTP 设备", userId);
        validTicket(userId,SecurityScene.BIND_TOTP,totpInitDTO.ticket());
        String secretKey = totpService.generateSecretKey();
        userTotpCredentialsService.saveOrUpdateUnactivated(userId,secretKey);
        // 获取该用户具备业务可读性的显示标识
        String username = userIdentifierService.findValueByUserIdAndType(userId, IdentifierType.USERNAME);
        if (username == null || username.isBlank()) {
            // 尝试拿邮箱兜底，实在没有就用 userId 字符形式
            username = userIdentifierService.findValueByUserIdAndType(userId, IdentifierType.EMAIL);
            if (username == null || username.isBlank()) {
                username = String.valueOf(userId);
            }
        }
        // 组装标准的 otpauth:// 协议长链接
        String otpAuthUrl = totpService.generateOtpAuthUrl(username, secretKey);
        return new TotpInitVO(otpAuthUrl, secretKey);
    }

    @Transactional(rollbackFor = Exception.class)
    public TotpActivateVO activateTotp(Long userId, TotpActivateDTO totpActivateDTO){
        log.info("用户 {} 提交首组验证码尝试激活 TOTP", userId);
        UserTotpCredentials credential = userTotpCredentialsService.getByUserId(userId);
        if (credential == null) {
            throw new BusinessException("未找到处于待激活状态的 TOTP 配置，请先发起初始化");
        }
        if (UserTotpStatus.ACTIVATED.equals(credential.getStatus())) {
            log.warn("用户 {} 尝试重复激活已开启的 TOTP 2FA", userId);
            throw new BusinessException("双因子认证已处于激活状态，请勿重复操作");
        }
        String secretKey = credential.getSecretKey();
        boolean isMatched = totpService.verify(secretKey, totpActivateDTO.code());
        if (!isMatched) {
            log.warn("用户 {} 激活 TOTP 失败：验证码错误或已过期", userId);
            throw new BusinessException("验证码不正确或已过期，请重新输入");
        }
        userTotpCredentialsService.updateStatus(userId, UserTotpStatus.ACTIVATED);
        // 生成备份码
        List<String> backupCodes = userMfaBackupCodeService.refreshBackupCodes(userId);
        log.info("用户 {} 的 TOTP 2FA 已成功激活并投入使用，备份码已同步同步下发", userId);
        return new TotpActivateVO(backupCodes);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindTotp(Long userId,TotpUnbindDTO totpUnbindDTO){
        log.info("用户 {} 正在请求彻底解绑关闭 TOTP 双因子认证", userId);
        validTicket(userId, SecurityScene.UNBIND_TOTP, totpUnbindDTO.ticket());
        // 门票检查通过，确认是否存在已激活的 2FA 记录
        UserTotpCredentials credential = userTotpCredentialsService.getByUserId(userId);
        if (credential == null) {
            return;
        }
        userTotpCredentialsService.removeByUserId(userId);
        // 移除备份码
        userMfaBackupCodeService.removeByUserId(userId);
        log.info("用户 {} 成功解绑并关闭了 TOTP 2FA 双因子验证", userId);
    }

    public MfaRefreshBackupCodeVO refreshTotpBackupCode(Long userId, MfaRefreshBackupCodeDTO mfaRefreshBackupCodeDTO){
        validTicket(userId, SecurityScene.GENERATE_TOTP_BACKUP_CODE, mfaRefreshBackupCodeDTO.ticket());
        List<String> backupCodes = userMfaBackupCodeService.refreshBackupCodes(userId);
        return new MfaRefreshBackupCodeVO(backupCodes);
    }

    public void bindGesture(Long userId,GestureBindDTO gestureBindDTO){
        if (!Objects.equals(gestureBindDTO.gesture(), gestureBindDTO.confirmGesture())) {
            throw new BusinessException("两次输入的手势密码不一致");
        }
        validTicket(userId,SecurityScene.BIND_GESTURE,gestureBindDTO.ticket());
        userGestureCredentialsService.saveOrUpdateGesture(userId,gestureBindDTO.gesture());
    }

    public void unbindGesture(Long userId,GestureUnbindDTO gestureUnbindDTO){
        validTicket(userId,SecurityScene.UNBIND_GESTURE,gestureUnbindDTO.ticket());
        userGestureCredentialsService.removeByUserId(userId);
    }

    public GestureVerifyVO verifyGesture(Long userId,GestureVerifyDTO gestureVerifyDTO){
        boolean verified = userGestureCredentialsService.matchGesture(userId, gestureVerifyDTO.gesture());
        String ticket = null;
        if (verified) {
            ticket = generateTicket(userId, gestureVerifyDTO.securityScene());
        }
        return new GestureVerifyVO(verified,ticket);
    }

    public void bindWeb3Wallet(Long userId, Web3WalletBindDTO web3WalletBindDTO){
        String ticket = web3WalletBindDTO.ticket();
        // 安全风控凭证核验
        validTicket(userId,SecurityScene.BIND_WEB3_WALLET, ticket);
        // 签名核验
        Web3WalletVerifySignatureResponse res = web3WalletService.verifySignature(new Web3WalletVerifySignatureDTO(web3WalletBindDTO.web3Id(), web3WalletBindDTO.signature()));
        // 验证通过的标准化钱包地址
        String address = res.address();
        Web3WalletType walletType = res.walletType();
        String label = res.label();
        String source = res.source();
        // 查询这个钱包地址在系统里有没有被别人占坑
        Optional<UserWeb3Credentials> existingCredentialOpt = userWeb3CredentialsService.getByAddress(address);
        if (existingCredentialOpt.isPresent()) {
            UserWeb3Credentials existingCredential = existingCredentialOpt.get();
            // 这个钱包已经被当前登录的用户自己绑定过了
            if (existingCredential.getUserId().equals(userId)) {
                log.info("【Web3 钱包绑定】用户 {} 重复绑定同一钱包 {}, 触发幂等直接返回成功", userId, address);
                return; // 幂等处理，直接返回成功，不重复落库
            }
            // 这个钱包已经被系统的【其他用户】给强占了
            throw new BusinessException("该加密钱包已被其他账号绑定，请先在原账号执行解绑");
        }
        boolean success = userWeb3CredentialsService.saveCredential(userId,address, walletType,label, source);
        if (!success) {
            throw new BusinessException("钱包绑定失败，数据写入异常");
        }
        log.info("【Web3 钱包核验成功】当前登录用户 userId: {}, 事务 web3Id: {}, " +
                        "反解出安全的钱包地址 address: {}, 钱包协议 walletType: {}, 请求来源 source: {}",
                userId, web3WalletBindDTO.web3Id(), address, walletType, source);
    }

    public Web3WalletVerifyVO verifyWeb3Wallet(Long userId,Web3WalletVerifyDTO web3WalletVerifyDTO){
        // 签名核验
        boolean verified = false;
        try {
            Web3WalletVerifySignatureResponse res = web3WalletService.verifySignature(new Web3WalletVerifySignatureDTO(web3WalletVerifyDTO.web3Id(), web3WalletVerifyDTO.signature()));
            Optional<UserWeb3Credentials> credentialsOpt = userWeb3CredentialsService.getByAddress(res.address());
            if(credentialsOpt.isPresent()){
                UserWeb3Credentials credentials = credentialsOpt.get();
                if (credentials.getUserId().equals(userId)) {
                    verified = true;
                }
            }
        }catch (Exception e){
            log.error("web3 wallet authenticate error", e);
        }
        String ticket = null;
        if (verified) {
            ticket = generateTicket(userId, web3WalletVerifyDTO.securityScene());
        }
        return new Web3WalletVerifyVO(verified,ticket);
    }

    public void unbindWeb3Wallet(Long userId, Web3WalletUnbindDTO web3WalletUnbindDTO){
        Long credentialId = web3WalletUnbindDTO.credentialId();
        String ticket = web3WalletUnbindDTO.ticket();
        // 安全风控凭证核验
        validTicket(userId,SecurityScene.UNBIND_WEB3_WALLET, ticket);
        UserWeb3Credentials credential = userWeb3CredentialsService.getById(credentialId);
        if (credential == null) {
            throw new BusinessException("未找到该钱包绑定记录");
        }
        if (!credential.getUserId().equals(userId)) {
            log.warn("【Web3 钱包解绑越权警告】用户 {} 尝试解绑不属于自己的凭证 ID: {}, 凭证实际所属用户: {}",
                    userId, credentialId, credential.getUserId());
            throw new BusinessException("操作非法，您无权解绑该钱包凭证");
        }
        // 防止孤儿账号
        ensureIdentityNotOrphaned(userId, CredentialType.WEB3, credentialId);
        // 执行解绑
        boolean success = userWeb3CredentialsService.removeCredential(userId, credential.getAddress());
        if (!success) {
            throw new BusinessException("钱包解绑失败，数据写入异常");
        }
        log.info("【Web3 钱包解绑成功】用户 userId: {}, 成功解除钱包地址: {} [协议: {}, 标签: {}] 的绑定关系",
                userId, credential.getAddress(), credential.getWalletType(), credential.getLabel());
    }

    public ThirdPartyProviderBindVO bindThirdPartyProvider(Long userId, ThirdPartyProviderBindDTO thirdPartyProviderBindDTO){
        // 校验ticket
        validTicket(userId,SecurityScene.BIND_THIRD_PARTY_PROVIDER,thirdPartyProviderBindDTO.ticket());
        SsoProviderAuthorizeUrlResponse response = thirdPartyLoginProviderFactory.getProvider(thirdPartyProviderBindDTO.provider()).getAuthorizeUrl(ThirdPartyAuthAction.BIND);
        return new ThirdPartyProviderBindVO(response.url(),response.pkceRequired());
    }

    public void unbindThirdPartyProvider(Long userId,ThirdPartyProviderUnbindDTO thirdPartyProviderUnbindDTO){
        String ticket = thirdPartyProviderUnbindDTO.ticket();
        // 安全风控凭证核验
        validTicket(userId,SecurityScene.UNBIND_THIRD_PARTY_PROVIDER, ticket);
        // 基础数据合法性及越权校验
        Long providerId = thirdPartyProviderUnbindDTO.providerId();
        UserProvider userProvider = userProviderService.getById(providerId);
        if(userProvider == null || !userProvider.getUserId().equals(userId)){
            throw new BusinessException("未找到该三方账号的绑定记录");
        }
        // 防止孤儿账号
        ensureIdentityNotOrphaned(userId, CredentialType.THIRD_PARTY, providerId);
        // 执行解绑
        userProviderService.removeById(providerId);
    }

    /**
     * 【核心风控门禁】所有第一身份凭证解绑前的生死存亡校验
     * @param userId 用户ID
     * @param credentialType 当前正在操作/准备解绑的凭证大类
     * @param targetId 当前准备删掉的凭证主键（如密码表id、WebAuthn的credential_id、Web3的id、三方表的id）
     */
    private void ensureIdentityNotOrphaned(Long userId, CredentialType credentialType, Object targetId){
        boolean canLoginAfterRemoval = credentialCheckers.stream()
                .anyMatch(checker -> {
                    // 如果轮询到了当前正在被解绑的这一类凭证，必须调用排除法计数
                    if (checker.getCredentialType() == credentialType) {
                        return checker.hasCredentialExcluding(userId, targetId);
                    }
                    // 其它类型的凭证，直接看用户手里还有没有（只要有任意一个其它类型的盾，就是安全的）
                    return checker.hasCredential(userId);
                });

        if (!canLoginAfterRemoval) {
            log.warn("【安全拦截】用户 {} 尝试解绑唯一的 {} 凭证(ID: {}), 已被系统拒绝", userId, credentialType, targetId);
            throw new BusinessException("操作失败：为了您的账号安全，请至少保留一种登录方式（密码、Passkey、钱包或其他社交绑定）");
        }
    }

    private String generateTicket(Long userId, SecurityScene securityScene) {
        String ticket = TicketGenerator.generate();
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
