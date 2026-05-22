package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.UserPasswordCredentials;
import com.atlas.auth.mapper.UserPasswordCredentialsMapper;
import com.atlas.auth.service.UserPasswordCredentialsService;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;


/**
 * (UserPasswordCredentials)表服务实现类
 *
 * @author ys
 * @since 2026-05-22 09:09:13
 */
@Service("userPasswordCredentialsService")
@RequiredArgsConstructor
@Slf4j
public class UserPasswordCredentialsServiceImpl extends ServiceImpl<UserPasswordCredentialsMapper, UserPasswordCredentials> implements UserPasswordCredentialsService {

    private final PasswordEncoder passwordEncoder;

    private static final int PASSWORD_EXPIRE_DAYS = 365;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPassword(Long userId, String rawPassword) {
        Objects.requireNonNull(userId, "用户ID不能为空");
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        LocalDateTime now = LocalDateTime.now();
        // 精准投影，高并发性能优化
        UserPasswordCredentials credentials = this.getOne(
                new LambdaQueryWrapper<UserPasswordCredentials>()
                        .select(UserPasswordCredentials::getId)
                        .eq(UserPasswordCredentials::getUserId, userId)
        );
        if (credentials == null) {
            credentials = new UserPasswordCredentials();
            credentials.setId(IdGen.genId());
            credentials.setUserId(userId);
            credentials.setPassword(hashedPassword);
            credentials.setLastChangedTime(now);
            credentials.setExpiredTime(now.plusDays(PASSWORD_EXPIRE_DAYS));
            try {
                this.save(credentials);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.warn("并发设置密码触发唯一键冲突，自动转为更新逻辑，userId: {}", userId);
                updatePasswordHash(userId, hashedPassword, now);
            }
        } else {
            updatePasswordHash(userId, hashedPassword, now);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, String oldRawPassword, String newRawPassword) {
        Objects.requireNonNull(userId, "用户ID不能为空");
        if (Objects.equals(oldRawPassword, newRawPassword)) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        boolean verify = verifyPassword(userId, oldRawPassword);
        // 严密校验旧密码
        if (verify) {
            throw new BusinessException("当前原密码输入不正确");
        }

        LocalDateTime now = LocalDateTime.now();
        updatePasswordHash(userId, passwordEncoder.encode(newRawPassword), now);
    }

    @Override
    public String getPasswordHashByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        UserPasswordCredentials credentials = this.getOne(
                new LambdaQueryWrapper<UserPasswordCredentials>()
                        .select(UserPasswordCredentials::getPassword)
                        .eq(UserPasswordCredentials::getUserId, userId)
        );
        return credentials != null ? credentials.getPassword() : null;
    }

    @Override
    public boolean hasPassword(Long userId) {
        String hash = this.getPasswordHashByUserId(userId);
        return hash != null && !hash.isBlank();
    }

    @Override
    public boolean verifyPassword(Long userId, String password) {
        Objects.requireNonNull(userId, "用户ID不能为空");
        if (StringUtils.isBlank(password)) {
            return false;
        }
        UserPasswordCredentials credentials = this.getOne(
                new LambdaQueryWrapper<UserPasswordCredentials>()
                        .eq(UserPasswordCredentials::getUserId, userId)
        );
        if (credentials == null) {
            log.warn("用户密码凭证不存在，用户ID: {}", userId);
            return false;
        }
        return passwordEncoder.matches(password, credentials.getPassword());
    }

    @Override
    public boolean isPasswordExpired(Long userId) {
        UserPasswordCredentials credentials = this.getOne(
                new LambdaQueryWrapper<UserPasswordCredentials>()
                        .select(UserPasswordCredentials::getExpiredTime)
                        .eq(UserPasswordCredentials::getUserId, userId)
        );
        if (credentials == null) {
            return true;
        }
        return credentials.getExpiredTime() != null && LocalDateTime.now().isAfter(credentials.getExpiredTime());
    }

    private void updatePasswordHash(Long userId, String hashedPassword, LocalDateTime now) {
        UserPasswordCredentials updateEntity = new UserPasswordCredentials();
        updateEntity.setPassword(hashedPassword);
        updateEntity.setLastChangedTime(now);
        updateEntity.setExpiredTime(now.plusDays(PASSWORD_EXPIRE_DAYS));
        this.update(updateEntity, new LambdaQueryWrapper<UserPasswordCredentials>()
                .eq(UserPasswordCredentials::getUserId, userId)
        );
    }

}

