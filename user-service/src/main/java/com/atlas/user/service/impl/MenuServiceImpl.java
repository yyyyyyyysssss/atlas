package com.atlas.user.service.impl;

import com.atlas.common.core.constant.CommonConstant;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.core.utils.TreeUtils;
import com.atlas.user.domain.dto.MenuCreateDTO;
import com.atlas.user.domain.dto.MenuDragDTO;
import com.atlas.user.domain.dto.MenuQueryDTO;
import com.atlas.user.domain.dto.MenuUpdateDTO;
import com.atlas.user.domain.entity.Authority;
import com.atlas.user.domain.entity.Role;
import com.atlas.user.domain.vo.MenuVO;
import com.atlas.user.domain.vo.RoleVO;
import com.atlas.user.enums.AuthorityAccessControl;
import com.atlas.user.enums.AuthorityDomain;
import com.atlas.user.enums.AuthorityType;
import com.atlas.user.mapper.AuthorityMapper;
import com.atlas.user.mapping.AuthorityMapping;
import com.atlas.user.service.MenuService;
import com.atlas.user.service.RoleAuthorityService;
import com.atlas.user.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/19 10:26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl extends AbstractAuthorityService implements MenuService {

    private final RoleService roleService;

    private final RoleAuthorityService roleAuthorityService;

    private final AuthorityMapper authorityMapper;

    private final ObjectProvider<MenuService> selfProvider;

    @Override
    @CacheEvict(value = "user:menu", allEntries = true)
    public Long createMenu(MenuCreateDTO menuCreateDTO) {
        Authority authority = AuthorityMapping.INSTANCE.toAuthority(menuCreateDTO);
        authority.setId(IdGen.genId());
        authority.setType(AuthorityType.MENU);
        if (authority.getParentId() != null && authority.getParentId() != CommonConstant.TREE_ROOT_PARENT_ID) {
            Authority selectAuthority = authorityMapper.selectById(authority.getParentId());
            authority.setRootId(selectAuthority.getRootId());
        } else {
            authority.setParentId(CommonConstant.TREE_ROOT_PARENT_ID);
            authority.setRootId(authority.getId());
        }
        if (authority.getSort() == null) {
            int maxSortOfChildren = getMaxSortOfChildren(authority.getParentId());
            authority.setSort(maxSortOfChildren + 1);
        }
        int insert = authorityMapper.insert(authority);
        return insert > 0 ? authority.getId() : null;
    }

    @Override
    @CacheEvict(value = "user:menu", allEntries = true)
    public Integer updateMenu(MenuUpdateDTO menuUpdateDTO) {
        Authority authority = authorityMapper.selectById(menuUpdateDTO.getId());
        if (authority == null || !authority.getType().equals(AuthorityType.MENU)) {
            throw new BusinessException("该菜单不存在");
        }
        AuthorityMapping.INSTANCE.updateAuthority(menuUpdateDTO, authority);
        return authorityMapper.updateById(authority);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user:menu", allEntries = true)
    public Boolean menuDrag(MenuDragDTO menuDragDTO) {
        String dragId = menuDragDTO.getDragId();
        String targetId = menuDragDTO.getTargetId();
        List<Authority> authorityList = authorityMapper.selectByIds(List.of(dragId, targetId));
        if (CollectionUtils.isEmpty(authorityList) || authorityList.size() != 2) {
            throw new BusinessException("菜单不存在");
        }
        Authority dragAuthority = authorityList.stream().filter(f -> f.getId().toString().equals(dragId)).findAny().orElseThrow(() -> new BusinessException("拖动的菜单不存在"));
        Authority targetAuthority = authorityList.stream().filter(f -> f.getId().toString().equals(targetId)).findAny().orElseThrow(() -> new BusinessException("目标菜单不存在"));
        MenuDragDTO.Position position = menuDragDTO.getPosition();
        UpdateWrapper<Authority> updateWrapper;
        switch (position) {
            case BEFORE, AFTER:
                //设置拖动节点的父节点以及根节点id为目标  节点的数据
                dragAuthority.setParentId(targetAuthority.getParentId());
                dragAuthority.setRootId(targetAuthority.getParentId() == 0 ? dragAuthority.getId() : targetAuthority.getRootId());
                //查出目标节点的所有兄弟节点
                QueryWrapper<Authority> queryWrapper = new QueryWrapper<>();
                queryWrapper
                        .lambda()
                        .eq(Authority::getParentId, targetAuthority.getParentId())
                        .eq(Authority::getType, AuthorityType.MENU)
                        .orderByAsc(Authority::getSort);
                List<Authority> authorities = authorityMapper.selectList(queryWrapper);
                //移出拖动的节点(如果存在)
                authorities.removeIf(r -> r.getId().toString().equals(dragId));
                int targetIndex = authorities.indexOf(targetAuthority);
                int insertIndex = position.equals(MenuDragDTO.Position.BEFORE) ? targetIndex : targetIndex + 1;
                if (insertIndex > authorities.size()) {
                    insertIndex = authorities.size();
                }
                authorities.add(insertIndex, dragAuthority);
                //根据拖动节点的索引重置兄弟节点的排序
                List<Authority> resetSortAuthorities = getResetSortAuthoritiesByIndex(insertIndex, authorities);
                return this.updateBatchById(resetSortAuthorities);
            case INSIDE:
                int minSortOfChildren = getMinSortOfChildren(targetAuthority.getId(), targetAuthority.getSort());
                updateWrapper = new UpdateWrapper<>();
                updateWrapper
                        .lambda()
                        .set(Authority::getSort, minSortOfChildren - 1)
                        .set(Authority::getParentId, targetId)
                        .set(Authority::getRootId, targetAuthority.getRootId())
                        .eq(Authority::getId, dragId);
                return authorityMapper.update(null, updateWrapper) > 0;
        }
        return false;
    }

    private List<Authority> getResetSortAuthoritiesByIndex(int index, List<Authority> authorities) {
        int prevIndex = Math.max(index - 1, 0);
        int sort = 0;
        List<Authority> resetAuthorityList = new ArrayList<>();
        for (int i = 0; i < authorities.size(); i++) {
            if (i < prevIndex) {
                continue;
            }
            Authority authority = authorities.get(i);
            if (i == prevIndex) {
                sort = authority.getSort();
            } else {
                authority.setSort(++sort);
            }
            resetAuthorityList.add(authority);
        }
        return resetAuthorityList;
    }

    @Override
    public List<MenuVO> tree() {

        return tree(AuthorityDomain.GLOBAL);
    }

    @Override
    public List<MenuVO> tree(AuthorityDomain domain) {
        QueryWrapper<Authority> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .lambda()
                .eq(Authority::getType, AuthorityType.MENU.name())
                .eq(Authority::getDomain, domain.getCode())
                .orderByAsc(Authority::getSort)
                .orderByAsc(Authority::getId);
        List<Authority> authorities = authorityMapper.selectList(queryWrapper);
        if (authorities == null || authorities.isEmpty()) {
            return new ArrayList<>();
        }
        List<MenuVO> menuVOList = AuthorityMapping.INSTANCE.toMenuVo(authorities);
        return TreeUtils.buildTree(
                menuVOList,
                MenuVO::getId,
                MenuVO::getParentId,
                MenuVO::setChildren,
                CommonConstant.TREE_ROOT_PARENT_ID
        );
    }

    @Override
    public MenuVO details(Long id) {
        Authority authority = authorityMapper.selectById(id);
        if (authority == null || !authority.getType().equals(AuthorityType.MENU)) {
            throw new BusinessException("该菜单不存在");
        }
        MenuVO menuVo = AuthorityMapping.INSTANCE.toMenuVo(authority);
        if (!menuVo.getParentId().equals(CommonConstant.TREE_ROOT_PARENT_ID)) {
            QueryWrapper<Authority> authorityQueryWrapper = new QueryWrapper<>();
            authorityQueryWrapper
                    .lambda()
                    .select(Authority::getCode)
                    .eq(Authority::getId, menuVo.getParentId());
            Authority parentAuthority = authorityMapper.selectOne(authorityQueryWrapper);
            menuVo.setParentName(parentAuthority.getCode());
        }
        return menuVo;
    }

    @Override
    public List<MenuVO> findByUserId(Long userId, AuthorityDomain domain) {
        List<MenuVO> menus = selfProvider.getObject().findByUserId(userId);
        return menus.stream()
                .filter(item -> item.getDomain().equals(domain))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "user:menu", key = "#p0")
    public List<MenuVO> findByUserId(Long userId) {
        // 获取用户拥有的角色
        List<RoleVO> roles = roleService.findByUserId(userId);
        List<Long> roleIds = roles.stream().map(RoleVO::getId).toList();
        QueryWrapper<Authority> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<Authority> lambdaWrapper = queryWrapper
                .lambda()
                .select(
                        Authority::getId,
                        Authority::getParentId,
                        Authority::getCode,
                        Authority::getName,
                        Authority::getRoutePath,
                        Authority::getDomain,
                        Authority::getIcon
                )
                .eq(Authority::getType, AuthorityType.MENU.name())
                .orderByAsc(Authority::getSort, Authority::getId);
        List<Authority> authorities;
        // 无角色返回
        if (CollectionUtils.isEmpty(roleIds)) {
            lambdaWrapper.eq(
                    Authority::getAccessControl,
                    AuthorityAccessControl.AUTHENTICATED.name()
            );
            authorities = authorityMapper.selectList(lambdaWrapper);
            return AuthorityMapping.INSTANCE.toMenuVo(authorities);
        }
        // 超级管理员直接返回所有菜单
        boolean isSuperAdmin = roles.stream().anyMatch(RoleVO::isSuperAdmin);
        if (isSuperAdmin) {
            authorities = authorityMapper.selectList(lambdaWrapper);
            return AuthorityMapping.INSTANCE.toMenuVo(authorities);
        }

        // 普通角色
        List<Long> authorityIds = roleAuthorityService.findAuthorityIdByRoleId(roleIds);
        lambdaWrapper.and(wrapper -> {
            // 公共菜单
            wrapper.eq(
                    Authority::getAccessControl,
                    AuthorityAccessControl.AUTHENTICATED.name()
            );
            // 授权菜单
            if (!CollectionUtils.isEmpty(authorityIds)) {
                wrapper.or()
                        .in(Authority::getId, authorityIds);
            }
        });
        authorities = authorityMapper.selectList(lambdaWrapper);
        return AuthorityMapping.INSTANCE.toMenuVo(authorities);
    }

    @Override
    @Transactional
    public Boolean deleteMenu(Long id) {
        //查询出菜单对应的所有子菜单或权限
        List<Authority> authorities = authorityMapper.selectChildrenById(id);
        if (authorities == null || authorities.isEmpty()) {
            throw new BusinessException("该菜单不存在");
        }
        Set<Long> delIds = authorities.stream().map(Authority::getId).collect(Collectors.toSet());
        int i = authorityMapper.deleteByIds(delIds);
        if (i != delIds.size()) {
            throw new BusinessException("删除菜单失败");
        }
        //删除菜单或权限与角色的关联关系
        for (Long delId : delIds) {
            roleService.bindAuthorityRole(delId, new ArrayList<>());
        }
        return true;
    }

}
