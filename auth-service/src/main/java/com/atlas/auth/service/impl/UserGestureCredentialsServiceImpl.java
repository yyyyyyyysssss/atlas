package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.UserGestureCredentials;
import com.atlas.auth.mapper.UserGestureCredentialsMapper;
import com.atlas.auth.service.UserGestureCredentialsService;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;


/**
 * (UserGestureCredentials)表服务实现类
 *
 * @author ys
 * @since 2026-06-04 08:58:22
 */
@Service("userGestureCredentialsService")
@RequiredArgsConstructor
@Slf4j
public class UserGestureCredentialsServiceImpl extends ServiceImpl<UserGestureCredentialsMapper, UserGestureCredentials> implements UserGestureCredentialsService {
    

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateGesture(Long userId, String plainGesture) {
        Objects.requireNonNull(userId, "用户id不能为空");
        Objects.requireNonNull(plainGesture, "手势不能为空");
        UserGestureCredentials credentials = this.lambdaQuery()
                .eq(UserGestureCredentials::getUserId, userId)
                .one();
        if (credentials == null) {
            credentials = new UserGestureCredentials();
            credentials.setId(IdGen.genId());
            credentials.setUserId(userId);
            credentials.setGesture(passwordEncoder.encode(plainGesture));
            return this.save(credentials);
        }
        credentials.setGesture(passwordEncoder.encode(plainGesture));
        return this.updateById(credentials);
    }

    @Override
    public boolean matchGesture(Long userId, String plainGesture) {
        Objects.requireNonNull(userId, "用户id不能为空");
        Objects.requireNonNull(plainGesture, "手势不能为空");
        UserGestureCredentials credentials = this.lambdaQuery()
                .eq(UserGestureCredentials::getUserId, userId)
                .one();
        if (credentials == null) {
            throw new BusinessException("该用户未绑定手势凭证");
        }
        return passwordEncoder.matches(plainGesture, credentials.getGesture());
    }

    @Override
    public Optional<UserGestureCredentials> getByUserId(Long userId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        return Optional.ofNullable(
                this.lambdaQuery()
                        .eq(UserGestureCredentials::getUserId, userId)
                        .one()
        );
    }

    @Override
    public boolean removeByUserId(Long userId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        return this.lambdaUpdate()
                .eq(UserGestureCredentials::getUserId, userId)
                .remove();
    }
}

