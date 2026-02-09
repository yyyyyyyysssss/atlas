package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.user.config.idwork.IdGen;
import com.atlas.user.domain.dto.RoleCreateDTO;
import com.atlas.user.domain.dto.RoleQueryDTO;
import com.atlas.user.domain.dto.RoleUpdateDTO;
import com.atlas.user.domain.entity.Role;
import com.atlas.user.domain.vo.RoleVO;
import com.atlas.user.enums.RoleType;
import com.atlas.user.mapper.RoleMapper;
import com.atlas.user.mapping.RoleMapping;
import com.atlas.user.service.RoleAuthorityService;
import com.atlas.user.service.RoleService;
import com.atlas.user.service.UserRoleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/3 15:36
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>  implements RoleService {

    private final RoleMapper roleMapper;

    private final RoleAuthorityService roleAuthorityService;

    private final UserRoleService userRoleService;

    @Override
    @Transactional
    public Long createRole(RoleCreateDTO roleCreateDTO) {
        Role role = RoleMapping.INSTANCE.toRole(roleCreateDTO);
        role.setId(IdGen.genId());
        role.setType(RoleType.NORMAL);
        int row = roleMapper.insert(role);
        if (row <= 0) {
            throw new BusinessException("创建角色失败");
        }
        if(!CollectionUtils.isEmpty(roleCreateDTO.getUserIds())){
            this.bindRoleUsers(role.getId(), roleCreateDTO.getUserIds());
        }
        if(!CollectionUtils.isEmpty(roleCreateDTO.getAuthorityIds())){
            this.bindRoleAuthorities(role.getId(), roleCreateDTO.getAuthorityIds());
        }
        return role.getId();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user:role", allEntries = true),
            @CacheEvict(value = "user:authority", allEntries = true),
            @CacheEvict(value = "user:menu", allEntries = true),
    })
    public Integer updateRole(RoleUpdateDTO roleUpdateDTO, Boolean isFullUpdate) {
        Role role = checkAndResult(roleUpdateDTO.getId());
        if(role.isSuperAdmin()){
            throw new BusinessException("超级管理员角色无法修改");
        }
        if(isFullUpdate){
            RoleMapping.INSTANCE.overwriteRole(roleUpdateDTO, role);
        } else {
            RoleMapping.INSTANCE.updateRole(roleUpdateDTO, role);
        }
        int i = roleMapper.updateById(role);
        if (i <= 0) {
            throw new BusinessException("更新角色失败");
        }
        if(isFullUpdate){
            // 更新角色关联的用户
            this.bindRoleUsers(role.getId(),roleUpdateDTO.getUserIds());
            // 更新角色关联的权限
            this.bindRoleAuthorities(role.getId(), roleUpdateDTO.getAuthorityIds());
        } else {
            if(!CollectionUtils.isEmpty(roleUpdateDTO.getUserIds())){
                this.bindRoleUsers(role.getId(),roleUpdateDTO.getUserIds());
            }
            if(!CollectionUtils.isEmpty(roleUpdateDTO.getAuthorityIds())){
                this.bindRoleAuthorities(role.getId(), roleUpdateDTO.getAuthorityIds());
            }
        }
        return i;
    }

    @Override
    public PageInfo<RoleVO> queryList(RoleQueryDTO queryDTO) {
        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        PageHelper.startPage(pageNum, pageSize);
        QueryWrapper<Role> roleQueryWrapper = getRoleQueryWrapper(queryDTO);
        List<Role> roles = roleMapper.selectList(roleQueryWrapper);
        if (roles == null || roles.isEmpty()) {
            return new PageInfo<>();
        }
        PageInfo<Role> rolePageInfo = PageInfo.of(roles);
        List<RoleVO> result = RoleMapping.INSTANCE.toRoleVO(roles);
        PageInfo<RoleVO> pageInfo = new PageInfo<>();
        pageInfo.setList(result);
        pageInfo.setTotal(rolePageInfo.getTotal());
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        return pageInfo;
    }

    @Override
    public RoleVO details(Long id) {
        Role role = checkAndResult(id);
        RoleVO roleVO = RoleMapping.INSTANCE.toRoleVO(role);
        // 查询角色对应的权限
        List<Long> authorityIds = roleAuthorityService.findAuthorityIdByRoleId(id);
        roleVO.setAuthorityIds(authorityIds);
        // 查询角色关联的用户
        List<Long> userIds = userRoleService.findUserIdByRoleId(id);
        roleVO.setUserIds(userIds);
        return roleVO;
    }

    @Override
    public List<Long> findUserIdById(Long roleId) {
        return userRoleService.findUserIdByRoleId(roleId);
    }

    // 删除角色
    @Override
    @Caching(evict = {
            @CacheEvict(value = "user:authority", allEntries = true),
            @CacheEvict(value = "user:menu", allEntries = true),
            @CacheEvict(value = "user:role", allEntries = true),
    })
    @Transactional
    public Boolean deleteRole(Long roleId) {
        Role role = checkAndResult(roleId);
        if(role.isSuperAdmin()){
            throw new BusinessException("超级管理员角色无法删除");
        }
        int i = roleMapper.deleteById(roleId);
        if(i <= 0){
            throw new BusinessException("删除角色失败，角色可能不存在");
        }
        // 解绑角色对应的权限
        this.bindRoleAuthorities(roleId, new ArrayList<>());
        // 解绑角色对应的用户
        this.bindRoleUsers(roleId, new ArrayList<>());
        return true;
    }

    // 角色绑定权限
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user:authority", allEntries = true),
            @CacheEvict(value = "user:menu", allEntries = true),
    })
    public Boolean bindRoleAuthorities(Long roleId, List<Long> authorityIds) {
        if (roleId == null) {
            log.warn("buildRoleAuthorities called with empty roleId");
            return true;
        }
        roleAuthorityService.addRoleAuthority(roleId,authorityIds);
        return true;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user:authority", allEntries = true),
            @CacheEvict(value = "user:menu", allEntries = true),
    })
    public Boolean bindAuthorityRole(Long authorityId, List<Long> roleIds) {
        if (authorityId == null) {
            log.warn("bindAuthorityRole called with empty authorityId");
            return true;
        }
        roleAuthorityService.addAuthorityRole(authorityId,roleIds);
        return true;
    }

    // 角色绑定用户
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user:role", allEntries = true),
            @CacheEvict(value = "user:authority", allEntries = true),
            @CacheEvict(value = "user:menu", allEntries = true),
    })
    public Boolean bindRoleUsers(Long roleId, List<Long> userIds) {
        userRoleService.addRoleUser(roleId,userIds);
        return true;
    }

    // 用户绑定角色
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user:role", key = "#userId"),
            @CacheEvict(value = "user:authority", key = "#userId"),
            @CacheEvict(value = "user:menu", key = "#userId"),
    })
    public Boolean bindUserRole(Long userId, Collection<Long> roleIds) {
        userRoleService.addUserRole(userId,roleIds);
        return true;
    }

    // 查询用户对应的角色
    @Override
    @Cacheable(value = "user:role", key = "#userId")
    public List<RoleVO> findByUserId(Long userId) {
        if (userId == null) {
            log.warn("findRoleByUserId called with null userId");
            return Collections.emptyList();
        }
        Collection<Long> roleIds = userRoleService.findRoleIdByUserId(userId);
        if(CollectionUtils.isEmpty(roleIds)){
            return Collections.emptyList();
        }
        return findRoleByIds(roleIds);
    }

    private List<RoleVO> findRoleByIds(Collection<Long> roleIds){
        if(CollectionUtils.isEmpty(roleIds)){
            return Collections.emptyList();
        }
        QueryWrapper<Role> roleQueryWrapper = new QueryWrapper<>();
        roleQueryWrapper
                .lambda()
                .in(Role::getId, roleIds)
                .eq(Role::getEnabled, true);
        List<Role> roles = roleMapper.selectList(roleQueryWrapper);
        return RoleMapping.INSTANCE.toRoleVO(roles);
    }

    @Override
    public List<RoleVO> listRoleOptions() {
        QueryWrapper<Role> roleQueryWrapper = new QueryWrapper<>();
        roleQueryWrapper
                .lambda()
                .select(Role::getId, Role::getName)
                .eq(Role::getType, RoleType.NORMAL)
                .eq(Role::getEnabled, true);
        List<Role> roles = roleMapper.selectList(roleQueryWrapper);
        return RoleMapping.INSTANCE.toRoleVO(roles);
    }

    private Role checkAndResult(Serializable id){
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private QueryWrapper<Role> getRoleQueryWrapper(RoleQueryDTO queryDTO) {
        QueryWrapper<Role> roleQueryWrapper = new QueryWrapper<>();
        roleQueryWrapper.eq("type", RoleType.NORMAL);
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            roleQueryWrapper
                    .lambda()
                    .like(Role::getName, queryDTO.getKeyword())
                    .or()
                    .like(Role::getCode, queryDTO.getKeyword());
        }
        if (queryDTO.getEnabled() != null) {
            roleQueryWrapper.eq("enabled", queryDTO.getEnabled());
        }
        roleQueryWrapper.orderByDesc("create_time");
        return roleQueryWrapper;
    }
}
