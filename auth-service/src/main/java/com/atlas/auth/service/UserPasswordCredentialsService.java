package com.atlas.auth.service;


import com.atlas.auth.domain.entity.UserPasswordCredentials;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * (UserPasswordCredentials)表服务接口
 *
 * @author ys
 * @since 2026-05-22 09:09:13
 */
public interface UserPasswordCredentialsService extends IService<UserPasswordCredentials> {


    /**
     * 初始化或强制重置密码（如注册、管理员重置）
     *
     * @param userId      用户ID
     * @param rawPassword 新明文密码
     */
    void setPassword(Long userId, String rawPassword);

    /**
     * 用户自助修改密码（内含强力的防重放、旧密码合法性严格校验）
     *
     * @param userId          用户ID
     * @param oldRawPassword  旧明文密码
     * @param newRawPassword  新明文密码
     */
    void updatePassword(Long userId, String oldRawPassword, String newRawPassword);

    /**
     * 安全提取 BCrypt 哈希串 (仅用于核心安全上下文，如 Spring Security UserDetailsService)
     *
     * @param userId 用户ID
     * @return 密码哈希
     */
    String getPasswordHashByUserId(Long userId);

    boolean hasPassword(Long userId);

    boolean verifyPassword(Long userId, String password);

    /**
     * 检查当前密码凭证是否已经过期
     */
    boolean isPasswordExpired(Long userId);

}

