package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.UserTotpCredentials;
import com.atlas.auth.enums.UserTotpStatus;
import com.atlas.auth.mapper.UserTotpCredentialsMapper;
import com.atlas.auth.service.UserTotpCredentialsService;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;


/**
 * (UserTotpCredentials)表服务实现类
 *
 * @author ys
 * @since 2026-05-27 14:56:07
 */
@Service("userTotpCredentialsService")
@RequiredArgsConstructor
@Slf4j
public class UserTotpCredentialsServiceImpl extends ServiceImpl<UserTotpCredentialsMapper, UserTotpCredentials> implements UserTotpCredentialsService {
    
    private final UserTotpCredentialsMapper userTotpCredentialsMapper;


    @Override
    public UserTotpCredentials getByUserId(Long userId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        return userTotpCredentialsMapper.selectOne(
                new LambdaQueryWrapper<UserTotpCredentials>().eq(UserTotpCredentials::getUserId, userId)
        );
    }

    @Override
    public UserTotpCredentials getActivatedByUserId(Long userId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        UserTotpCredentials userTotpCredentials = getByUserId(userId);
        if (userTotpCredentials == null || userTotpCredentials.getStatus().equals(UserTotpStatus.UNACTIVATED)){
            return null;
        }
        return userTotpCredentials;
    }

    @Override
    public void saveOrUpdateUnactivated(Long userId, String secretKey) {
        Objects.requireNonNull(userId, "用户id不能为空");
        UserTotpCredentials record = this.getByUserId(userId);
        if (record == null) {
            record = new UserTotpCredentials();
            record.setId(IdGen.genId());
            record.setUserId(userId);
            record.setSecretKey(secretKey);
            record.setStatus(UserTotpStatus.UNACTIVATED);
            record.setIssuer("Atlas");
            userTotpCredentialsMapper.insert(record);
        } else {
            UserTotpCredentials updateRecord = new UserTotpCredentials();
            updateRecord.setSecretKey(secretKey);
            updateRecord.setStatus(UserTotpStatus.UNACTIVATED);
            userTotpCredentialsMapper.update(updateRecord, new LambdaUpdateWrapper<UserTotpCredentials>()
                    .eq(UserTotpCredentials::getUserId, userId)
            );
        }
    }

    @Override
    public void updateStatus(Long userId, UserTotpStatus status) {
        Objects.requireNonNull(userId, "用户id不能为空");
        UserTotpCredentials updateRecord = new UserTotpCredentials();
        updateRecord.setStatus(status);
        userTotpCredentialsMapper.update(updateRecord, new LambdaUpdateWrapper<UserTotpCredentials>()
                .eq(UserTotpCredentials::getUserId, userId)
        );
    }

    @Override
    public boolean removeByUserId(Long userId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        return this.remove(new LambdaQueryWrapper<UserTotpCredentials>()
                .eq(UserTotpCredentials::getUserId, userId)
        );
    }
    
}

