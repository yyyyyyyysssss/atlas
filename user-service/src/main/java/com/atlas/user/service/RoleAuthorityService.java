package com.atlas.user.service;

import com.atlas.user.domain.entity.RoleAuthority;
import com.baomidou.mybatisplus.extension.service.IService;


import java.util.Collection;
import java.util.List;

public interface RoleAuthorityService extends IService<RoleAuthority> {

    List<Long> findRoleIdByAuthorityId(Long authorityId);

    List<Long> findAuthorityIdByRoleId(Long roleId);

    List<Long> findAuthorityIdByRoleId(Collection<Long> roleIds);

    void addAuthorityRole(Long authorityId, Collection<Long> roleIds);

    void addRoleAuthority(Long roleId, Collection<Long> authorityIds);

}
