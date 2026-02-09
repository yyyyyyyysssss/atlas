package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.user.config.idwork.IdGen;
import com.atlas.user.domain.entity.RoleAuthority;
import com.atlas.user.mapper.RoleAuthorityMapper;
import com.atlas.user.service.RoleAuthorityService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/5 10:01
 */
@Service
@RequiredArgsConstructor
public class RoleAuthorityServiceImpl extends ServiceImpl<RoleAuthorityMapper, RoleAuthority> implements RoleAuthorityService {

    @Override
    public List<Long> findRoleIdByAuthorityId(Long authorityId) {
        if (authorityId == null) {
            log.warn("findRoleIdByAuthorityId called with null authorityId");
            return Collections.emptyList();
        }
        QueryWrapper<RoleAuthority> roleAuthorityQueryWrapper = new QueryWrapper<>();
        roleAuthorityQueryWrapper
                .lambda()
                .select(RoleAuthority::getRoleId)
                .eq(RoleAuthority::getAuthorityId, authorityId);
        List<RoleAuthority> roleAuthorities = this.list(roleAuthorityQueryWrapper);
        if (CollectionUtils.isEmpty(roleAuthorities)) {
            return Collections.emptyList();
        }
        return roleAuthorities.stream()
                .map(RoleAuthority::getRoleId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> findAuthorityIdByRoleId(Long roleId) {
        if (roleId == null) {
            log.warn("findAuthorityIdBy called with null roleId");
            return Collections.emptyList();
        }
        return findAuthorityIdByRoleId(Collections.singletonList(roleId));
    }

    @Override
    public List<Long> findAuthorityIdByRoleId(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            log.warn("findAuthorityIdByRoleId called with empty roleIds");
            return Collections.emptyList();
        }
        QueryWrapper<RoleAuthority> roleAuthorityQueryWrapper = new QueryWrapper<>();
        roleAuthorityQueryWrapper
                .lambda()
                .select(RoleAuthority::getAuthorityId)
                .in(RoleAuthority::getRoleId, roleIds);
        List<RoleAuthority> roleAuthorities = this.list(roleAuthorityQueryWrapper);
        if (CollectionUtils.isEmpty(roleAuthorities)) {
            return Collections.emptyList();
        }
        return roleAuthorities.stream()
                .map(RoleAuthority::getAuthorityId)
                .distinct()
                .toList();
    }

    @Override
    @Transactional
    public void addAuthorityRole(Long authorityId, Collection<Long> roleIds) {
        List<Long> roleIdList = this.findRoleIdByAuthorityId(authorityId);
        // 先删除
        removeRoleAuthority(roleIdList,Collections.singleton(authorityId));
        // 再新增
        addRoleAuthority(roleIds,Collections.singleton(authorityId));
    }

    @Override
    @Transactional
    public void addRoleAuthority(Long roleId, Collection<Long> authorityIds) {
        List<Long> authorityIdList = this.findAuthorityIdByRoleId(roleId);
        // 先删除
        removeRoleAuthority(Collections.singleton(roleId),authorityIdList);
        // 再新增
        addRoleAuthority(Collections.singleton(roleId),authorityIds);
    }

    @Transactional
    public void removeRoleAuthority(Collection<Long> roleIds, Collection<Long> authorityIds){
        if (CollectionUtils.isEmpty(roleIds) && CollectionUtils.isEmpty(authorityIds)){
            throw new BusinessException("角色绑定权限失败,角色或权限不能同时为空");
        }
        QueryWrapper<RoleAuthority> roleAuthorityQueryWrapper = new QueryWrapper<>();
        if(!CollectionUtils.isEmpty(roleIds)){
            roleAuthorityQueryWrapper.lambda().in(RoleAuthority::getRoleId, roleIds);
        }
        if(!CollectionUtils.isEmpty(authorityIds)){
            roleAuthorityQueryWrapper.lambda().in(RoleAuthority::getAuthorityId, authorityIds);
        }
        this.remove(roleAuthorityQueryWrapper);
    }

    @Transactional
    public void addRoleAuthority(Collection<Long> roleIds, Collection<Long> authorityIds) {
        // 再新增
        if(!CollectionUtils.isEmpty(roleIds) && !CollectionUtils.isEmpty(authorityIds)){
            List<RoleAuthority> roleAuthorities = new ArrayList<>();
            for (Long roleId : roleIds){
                for (Long authorityId : authorityIds){
                    RoleAuthority roleAuthority = new RoleAuthority();
                    roleAuthority.setId(IdGen.genId());
                    roleAuthority.setRoleId(roleId);
                    roleAuthority.setAuthorityId(authorityId);
                    roleAuthorities.add(roleAuthority);
                }
            }
            this.saveBatch(roleAuthorities);
        }
    }

}
