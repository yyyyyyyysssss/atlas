package com.atlas.user.service;

import com.atlas.user.domain.entity.UserRole;
import com.baomidou.mybatisplus.extension.service.IService;


import java.util.Collection;
import java.util.List;

public interface UserRoleService extends IService<UserRole> {


    List<Long> findRoleIdByUserId(Long userId);

    List<Long> findUserIdByRoleId(Long roleId);

    void addUserRole(Long userId, Collection<Long> roleIds);

    void addRoleUser(Long roleId,Collection<Long> userIds);

}
