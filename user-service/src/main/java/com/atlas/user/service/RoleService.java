package com.atlas.user.service;

import com.atlas.user.domain.dto.RoleCreateDTO;
import com.atlas.user.domain.dto.RoleQueryDTO;
import com.atlas.user.domain.dto.RoleUpdateDTO;
import com.atlas.user.domain.entity.Role;
import com.atlas.user.domain.vo.RoleVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;


import java.util.Collection;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/3 15:36
 */
public interface RoleService extends IService<Role> {

    Long createRole(RoleCreateDTO roleCreateDTO);

    Integer updateRole(RoleUpdateDTO roleUpdateDTO, Boolean isFullUpdate);

    PageInfo<RoleVO> queryList(RoleQueryDTO queryDTO);

    RoleVO details(Long id);

    Boolean deleteRole(Long roleId);

    List<Long> findUserIdById(Long roleId);

    Boolean bindRoleAuthorities(Long roleId, List<Long> authorityIds);

    Boolean bindAuthorityRole(Long authorityId, List<Long> roleIds);

    Boolean bindRoleUsers(Long roleId, List<Long> userIds);

    Boolean bindUserRole(Long userId, Collection<Long> roleIds);

    List<RoleVO> findByUserId(Long userId);

    List<RoleVO> listRoleOptions();

}
