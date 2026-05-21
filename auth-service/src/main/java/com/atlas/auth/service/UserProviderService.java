package com.atlas.auth.service;


import com.atlas.auth.domain.dto.UserProviderDTO;
import com.atlas.auth.domain.entity.UserProvider;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * (UserProvider)表服务接口
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
public interface UserProviderService extends IService<UserProvider> {

    UserProviderDTO getByProvider(String identityType, String identifier);

    void addUserProvider(Long userId, String provider, String providerUserId, Map<String, Object> extraInfo);

    List<UserProvider> listByUserId(Long userId);
}

