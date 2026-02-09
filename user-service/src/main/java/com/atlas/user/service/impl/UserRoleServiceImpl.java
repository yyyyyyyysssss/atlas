package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.user.config.idwork.IdGen;
import com.atlas.user.domain.entity.UserRole;
import com.atlas.user.mapper.UserRoleMapper;
import com.atlas.user.service.UserRoleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * @Date 2025/6/6 13:34
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {


    @Override
    public List<Long> findRoleIdByUserId(Long userId) {
        if (userId == null) {
            log.warn("findRoleIdByUserId called with null userId");
            return Collections.emptyList();
        }
        QueryWrapper<UserRole> userRoleQueryWrapper = new QueryWrapper<>();
        userRoleQueryWrapper
                .lambda()
                .select(UserRole::getRoleId)
                .eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = this.list(userRoleQueryWrapper);
        if (CollectionUtils.isEmpty(userRoles)) {
            return Collections.emptyList();
        }
        return userRoles.stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> findUserIdByRoleId(Long roleId) {
        if (roleId == null) {
            log.warn("findUserIdByRoleId called with null roleId");
            return Collections.emptyList();
        }
        QueryWrapper<UserRole> userRoleQueryWrapper = new QueryWrapper<>();
        userRoleQueryWrapper
                .lambda()
                .select(UserRole::getUserId)
                .eq(UserRole::getRoleId, roleId);
        List<UserRole> userRoles = this.list(userRoleQueryWrapper);
        if (CollectionUtils.isEmpty(userRoles)) {
            return Collections.emptyList();
        }
        return userRoles.stream()
                .map(UserRole::getUserId)
                .distinct()
                .toList();
    }

    @Override
    @Transactional
    public void addUserRole(Long userId, Collection<Long> roleIds) {
        List<Long> roleIdList = this.findRoleIdByUserId(userId);
        // 先删除
        removeUserRole(Collections.singleton(userId),roleIdList);
        // 再新增
        addUserRole(Collections.singleton(userId),roleIds);
    }

    @Override
    @Transactional
    public void addRoleUser(Long roleId, Collection<Long> userIds) {
        List<Long> userIdList = this.findUserIdByRoleId(roleId);
        // 先删除
        removeUserRole(userIdList,Collections.singletonList(roleId));
        // 再新增
        addUserRole(userIds,Collections.singletonList(roleId));
    }

    @Transactional
    public void removeUserRole(Collection<Long> userIds, Collection<Long> roleIds){
        if (CollectionUtils.isEmpty(userIds) && CollectionUtils.isEmpty(roleIds)){
            throw new BusinessException("用户角色绑定失败,用户或角色不能同时为空");
        }
        QueryWrapper<UserRole> userRoleQueryWrapper = new QueryWrapper<>();
        if(!CollectionUtils.isEmpty(userIds)){
            userRoleQueryWrapper.lambda().in(UserRole::getUserId, userIds);
        }
        if(!CollectionUtils.isEmpty(roleIds)){
            userRoleQueryWrapper.lambda().in(UserRole::getRoleId, roleIds);
        }
        this.remove(userRoleQueryWrapper);
    }

    @Transactional
    public void addUserRole(Collection<Long> userIds, Collection<Long> roleIds) {
        // 再新增
        if(!CollectionUtils.isEmpty(userIds) && !CollectionUtils.isEmpty(roleIds)){
            List<UserRole> userRoles = new ArrayList<>();
            for (Long userId : userIds){
                for (Long roleId : roleIds){
                    UserRole userRole = new UserRole();
                    userRole.setId(IdGen.genId());
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    userRoles.add(userRole);
                }
            }
            this.saveBatch(userRoles);
        }
    }
}
