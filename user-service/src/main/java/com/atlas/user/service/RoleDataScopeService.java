package com.atlas.user.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.atlas.user.domain.entity.RoleDataScope;
import com.atlas.user.domain.dto.RoleDataScopeDTO;

import java.util.Collection;
import java.util.List;

/**
 * (RoleDataScope)表服务接口
 *
 * @author ys
 * @since 2026-03-17 13:31:51
 */
public interface RoleDataScopeService extends IService<RoleDataScope> {

    void addRoleDataScope(Long roleId, List<Long> orgIds);

    void deleteRoleDataScope(Long roleId);

    List<RoleDataScopeDTO> findRoleDataScope(Long roleId);

    List<RoleDataScopeDTO> findRoleDataScope(Collection<Long> roleIds);
}

