package com.atlas.auth.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TotpService {

    private final GoogleAuthenticator googleAuthenticator;

    private static final String DEFAULT_ISSUER = "Atlas";

    /**
     * 生成一个新的 TOTP 密钥 (Secret Key)
     */
    public String generateSecretKey() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    /**
     * 根据账户标识生成标准的 otpauth:// 协议字符串（用于前端生成二维码）
     * * @param accountIdentifier 账号唯一标识（可以是 userId 的字符串形式）
     * @param secretKey         未加密的原始密钥
     */
    public String generateOtpAuthUrl(String accountIdentifier, String secretKey) {

        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                DEFAULT_ISSUER,
                accountIdentifier,
                new GoogleAuthenticatorKey.Builder(secretKey).build()
        );
    }

    /**
     * 纯算法校验：只验证密钥和 6 位 Code 是否匹配
     * * @param secretKey 未加密的原始密钥
     * @param totpCode  用户输入的 6 位数字验证码
     */
    public boolean verify(String secretKey, Integer totpCode) {
        if (secretKey == null || totpCode == null) {
            return false;
        }
        try {
            return googleAuthenticator.authorize(secretKey, totpCode);
        } catch (Exception e) {
            log.error("[TOTP] 验证算法执行异常", e);
            return false;
        }
    }


}
