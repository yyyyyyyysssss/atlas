package com.atlas.auth.service;


import com.atlas.auth.domain.entity.UserTotpCredentials;
import com.atlas.auth.enums.UserTotpStatus;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * (UserTotpCredentials)表服务接口
 *
 * @author ys
 * @since 2026-05-27 14:56:06
 */
public interface UserTotpCredentialsService extends IService<UserTotpCredentials> {

    UserTotpCredentials getByUserId(Long userId);

    void saveOrUpdateUnactivated(Long userId, String secretKey);

    void updateStatus(Long userId, UserTotpStatus status);

    boolean removeByUserId(Long userId);
}

