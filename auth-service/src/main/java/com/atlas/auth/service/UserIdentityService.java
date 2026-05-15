package com.atlas.auth.service;


import com.atlas.auth.domain.dto.UserIdentityDTO;
import com.atlas.auth.domain.entity.UserIdentity;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * (UserIdentity)表服务接口
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
public interface UserIdentityService extends IService<UserIdentity> {

    UserIdentityDTO getByIdentity(String identityType, String identifier);

    void addUserIdentity(Long userId, ExternalIdentityDTO externalIdentityDTO);
}

