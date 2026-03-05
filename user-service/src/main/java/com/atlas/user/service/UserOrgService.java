package com.atlas.user.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.atlas.user.domain.entity.UserOrg;
import com.atlas.user.domain.dto.UserOrgDTO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * (UserOrg)表服务接口
 *
 * @author ys
 * @since 2026-03-03 11:08:40
 */
public interface UserOrgService extends IService<UserOrg> {

    void addUserOrg(List<UserOrgDTO> list);


    UserOrg findUserOrgMain(Long userId);

    void deleteOrgUser(Long orgId, List<Long> userIds);

    List<UserOrgDTO> findByUserId(Collection<Long> userIds);

    List<UserOrgDTO> findByOrgId(Collection<Long> orgIds);

    default List<UserOrgDTO> findByUserId(Long userId) {
        return findByUserId(Collections.singleton(userId));
    }

    default List<UserOrgDTO> findByOrgId(Long orgId) {
        return findByOrgId(Collections.singleton(orgId));
    }
}

