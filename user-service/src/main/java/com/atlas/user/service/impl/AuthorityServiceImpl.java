package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.TreeUtils;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.user.domain.dto.AuthorityCreateDTO;
import com.atlas.user.domain.dto.AuthorityUpdateDTO;
import com.atlas.user.domain.dto.AuthorityUrlDTO;
import com.atlas.user.domain.entity.Authority;
import com.atlas.user.domain.entity.AuthorityUrl;
import com.atlas.user.domain.vo.AuthorityVO;
import com.atlas.user.domain.vo.RoleVO;
import com.atlas.user.enums.AuthorityType;
import com.atlas.user.mapper.AuthorityMapper;
import com.atlas.user.mapping.AuthorityMapping;
import com.atlas.user.mapping.AuthorityUrlMapping;
import com.atlas.user.service.AuthorityService;
import com.atlas.user.service.AuthorityUrlService;
import com.atlas.user.service.RoleAuthorityService;
import com.atlas.user.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/16 23:35
 */
@Service
@Slf4j
public class AuthorityServiceImpl extends AbstractAuthorityService implements AuthorityService {

    @Resource
    private AuthorityMapper authorityMapper;

    @Resource
    private RoleService roleService;

    @Resource
    private RoleAuthorityService roleAuthorityService;

    @Resource
    private AuthorityUrlService authorityUrlService;

    @Override
    public Long createAuthority(AuthorityCreateDTO authorityAddDTO) {
        Authority authority = AuthorityMapping.INSTANCE.toAuthority(authorityAddDTO);
        authority.setId(IdGen.genId());
        authority.setType(AuthorityType.ACTION);
        Authority selectAuthority = authorityMapper.selectById(authority.getParentId());
        authority.setRootId(selectAuthority.getRootId());
        if (authority.getSort() == null){
            Long parentId = authority.getParentId();
            if (authority.getParentId() == null){
                parentId = 0L;
            }
            int maxSortOfChildren = getMaxSortOfChildren(parentId);
            authority.setSort(maxSortOfChildren + 1);
        }
        int insert = authorityMapper.insert(authority);
        return insert > 0 ? authority.getId() : null;
    }

    @Override
    public Boolean updateAuthority(AuthorityUpdateDTO authorityUpdateDTO, Boolean isFullUpdate) {
        Authority authority = authorityMapper.selectById(authorityUpdateDTO.getId());
        if (authority == null || !authority.getType().equals(AuthorityType.ACTION)) {
            throw new BusinessException("该操作权限不存在");
        }
        if (isFullUpdate){
            AuthorityMapping.INSTANCE.overwriteAuthority(authorityUpdateDTO,authority);
        } else {
            AuthorityMapping.INSTANCE.updateAuthority(authorityUpdateDTO,authority);
        }
        if(authorityUpdateDTO.getParentId() != null && !authorityUpdateDTO.getParentId().isEmpty() && !authorityUpdateDTO.getParentId().equals(authority.getParentId().toString())){
            Authority selectAuthority = authorityMapper.selectById(authorityUpdateDTO.getParentId());
            authority.setRootId(selectAuthority.getRootId());
        }
        return authorityMapper.updateById(authority) > 0;
    }

    @Override
    public AuthorityVO details(String id) {
        Authority authority = authorityMapper.selectById(id);
        return AuthorityMapping.INSTANCE.toAuthorityVO(authority);
    }

    @Override
    public List<AuthorityVO> findByMenuId(Long menuId) {
        if(menuId == null){
            throw new NullPointerException("menuId is null");
        }
        QueryWrapper<Authority> authorityQueryWrapper = new QueryWrapper<>();
        authorityQueryWrapper
                .lambda()
                .eq(Authority::getType,AuthorityType.ACTION)
                .eq(Authority::getParentId,menuId);
        List<Authority> authorities = authorityMapper.selectList(authorityQueryWrapper);
        if (CollectionUtils.isEmpty(authorities)){
            return Collections.emptyList();
        }
        return AuthorityMapping.INSTANCE.toAuthorityVO(authorities);
    }

    @Override
    public List<AuthorityVO> tree() {
        QueryWrapper<Authority> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .lambda()
                .select(Authority::getId, Authority::getParentId, Authority::getName)
                .in(Authority::getType, AuthorityType.MENU, AuthorityType.ACTION)
                .orderByAsc(Authority::getSort, Authority::getId);
        List<Authority> authorities = authorityMapper.selectList(queryWrapper);
        if (authorities == null || authorities.isEmpty()){
            return new ArrayList<>();
        }
        List<AuthorityVO> authorityList = AuthorityMapping.INSTANCE.toAuthorityVO(authorities);
        return TreeUtils.buildTree(
                authorityList,
                AuthorityVO::getId,
                AuthorityVO::getParentId,
                AuthorityVO::setChildren,
                0L
        );
    }

    @Override
    @Transactional
    public Boolean deleteAuthority(Long id) {
        Authority authority = authorityMapper.selectById(id);
        if (authority == null || !authority.getType().equals(AuthorityType.ACTION)){
            throw new BusinessException("该权限不存在");
        }
        int i = authorityMapper.deleteById(id);
        if(i <= 0){
            throw new BusinessException("删除权限失败");
        }
        // 解绑该权限与角色的关联关系
        roleService.bindAuthorityRole(id, new ArrayList<>());
        return true;
    }

    // 根据角色ID查询权限
    @Override
    public List<AuthorityVO> findByRoleId(Long roleId) {
        if(roleId == null){
            return Collections.emptyList();
        }
        return this.findByRoleId(Collections.singletonList(roleId));
    }

    // 根据用户ID查询权限
    @Override
    @Cacheable(value = "user:authority", key = "#p0")
    public List<AuthorityVO> findByUserId(Long userId) {
        if(userId == null){
            return Collections.emptyList();
        }
        List<RoleVO> roles = roleService.findByUserId(userId);
        if(CollectionUtils.isEmpty(roles)){
            return Collections.emptyList();
        }
        List<Long> roleIds = roles.stream().map(RoleVO::getId).toList();
        return this.findByRoleId(roleIds);
    }

    @Caching(evict = {
            @CacheEvict(value = "user:menu", key = "#p0"),
            @CacheEvict(value = "user:authority", key = "#p0")
    })
    public void clearCache(Long userId) {

    }

    @Override
    public List<AuthorityUrlDTO> getAuthorityUrl(Long id) {

        return authorityUrlService.findAuthorityUrl(id);
    }

    @Override
    public Long saveAuthorityUrl(Long id, AuthorityUrlDTO authorityUrlDTO) {
        if (authorityUrlDTO.getAuthorityId() != null && !authorityUrlDTO.getAuthorityId().equals(id)) {
            throw new BusinessException("参数错误：权限ID不匹配");
        }
        AuthorityUrl authorityUrl;
        if(authorityUrlDTO.getId() == null){
            authorityUrl = AuthorityUrlMapping.INSTANCE.toAuthorityUrl(authorityUrlDTO);
            authorityUrl.setId(IdGen.genId());
            authorityUrl.setAuthorityId(id);
            authorityUrlService.save(authorityUrl);
        } else {
            authorityUrl = authorityUrlService.getById(authorityUrlDTO.getId());
            if(authorityUrl == null){
                throw new BusinessException("该权限url不存在");
            }
            if(!authorityUrl.getAuthorityId().equals(id)){
                throw new BusinessException("该权限url不属于当前权限");
            }
            authorityUrl.setUrl(authorityUrlDTO.getUrl());
            authorityUrl.setMethod(authorityUrlDTO.getMethod());
            authorityUrlService.updateById(authorityUrl);
        }
        return authorityUrl.getId();
    }

    @Override
    public void deleteAuthorityUrl(Long id, Long authorityUrlId) {
        boolean removed = authorityUrlService.lambdaUpdate()
                .eq(AuthorityUrl::getAuthorityId, id)
                .eq(AuthorityUrl::getId, authorityUrlId)
                .remove();
        if (!removed) {
            throw new BusinessException("删除失败，数据不存在或无权操作");
        }
    }

    private List<AuthorityVO> findByRoleId(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<Long> authorityIds = roleAuthorityService.findAuthorityIdByRoleId(roleIds);
        if (CollectionUtils.isEmpty(authorityIds)){
            return Collections.emptyList();
        }
        List<Authority> authorities = authorityMapper.selectByIds(authorityIds);
        if (CollectionUtils.isEmpty(authorities)) {
            return Collections.emptyList();
        }
        List<AuthorityUrlDTO> urlDTOList = authorityUrlService.findAuthorityUrl(authorityIds);
        return AuthorityMapping.INSTANCE.toAuthorityVO(authorities);
    }
}
