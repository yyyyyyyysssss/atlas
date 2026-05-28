package com.atlas.auth.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/21 14:23
 */
@Data
@Builder
public class AccountSecurityVO {

    private String username;

    private Boolean isUsernameModified;

    /**
     * 是否已设置登录密码（用于区分第三方直接注册无密码的用户）
     */
    private Boolean passwordSet;

    /**
     * 已绑定的邮箱地址（如果有值返回脱敏后的邮箱如 y***@atlas.com，没有则返回 null）
     */
    private String boundEmail;

    /**
     * 邮箱是否已验证
     */
    private Boolean emailVerified;

    /**
     * 已绑定的手机号（脱敏后，如 182****8888，未绑定返回 null）
     */
    private String boundPhone;

    /**
     * 手机号是否已验证
     */
    private Boolean phoneVerified;


    private List<UserPasskeyVO> passkeys;

    private Boolean passkeyEnabled;

    /**
     * 第三方账号
     */
    private List<UserProviderVO> providers;

    // ================== 两步验证 (2FA / MFA) ==================

    /**
     * TOTP（身份验证器应用）是否已开启
     */
    private Boolean totpEnabled;

    /**
     * 是否已生成恢复码（通常 mfaEnabled 为 true 时，它也为 true）
     */
    private Boolean backupCodeGenerated;

}
