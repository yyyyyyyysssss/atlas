package com.atlas.user.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.atlas.user.domain.entity.UserIdentity;
import com.atlas.user.domain.dto.UserIdentityDTO;

import java.util.Collection;
import java.util.List;

/**
 * (UserIdentity)表服务接口
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
public interface UserIdentityService extends IService<UserIdentity> {

    UserIdentityDTO getByIdentity(String identityType, String identifier);

    void addUserIdentity(Long masterId, List<UserIdentityDTO> list);

    void deleteUserIdentity(Long masterId);

    List<UserIdentityDTO> findUserIdentity(Long masterId);

    List<UserIdentityDTO> findUserIdentity(Collection<Long> masterIds);
}

