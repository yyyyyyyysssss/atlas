package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.core.utils.TreeUtils;
import com.atlas.user.domain.dto.AuthorityCreateDTO;
import com.atlas.user.domain.dto.AuthorityUpdateDTO;
import com.atlas.user.domain.dto.AuthorityUrlDTO;
import com.atlas.user.domain.entity.Authority;
import com.atlas.user.domain.entity.AuthorityUrl;
import com.atlas.user.domain.vo.AuthorityVO;
import com.atlas.user.domain.vo.RoleVO;
import com.atlas.user.enums.AuthorityAccessControl;
import com.atlas.user.enums.AuthorityDomain;
import com.atlas.user.enums.AuthorityType;
import com.atlas.user.mapper.AuthorityMapper;
import com.atlas.user.mapping.AuthorityMapping;
import com.atlas.user.mapping.AuthorityUrlMapping;
import com.atlas.user.service.AuthorityService;
import com.atlas.user.service.AuthorityUrlService;
import com.atlas.user.service.RoleAuthorityService;
import com.atlas.user.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/16 23:35
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorityServiceImpl extends AbstractAuthorityService implements AuthorityService {

    private final AuthorityMapper authorityMapper;

    private final RoleService roleService;

    private final RoleAuthorityService roleAuthorityService;

    private final AuthorityUrlService authorityUrlService;

    private final ObjectProvider<AuthorityService> selfProvider;

    @Override
    @Transactional
    public Long createAuthority(AuthorityCreateDTO authorityAddDTO) {
        Authority authority = AuthorityMapping.INSTANCE.toAuthority(authorityAddDTO);
        authority.setId(IdGen.genId());
        authority.setType(AuthorityType.ACTION);
        Authority selectAuthority = authorityMapper.selectById(authority.getParentId());
        authority.setRootId(selectAuthority.getRootId());
        authority.setAccessControl(selectAuthority.getAccessControl());
        if (authority.getSort() == null){
            Long parentId = authority.getParentId();
            if (authority.getParentId() == null){
                parentId = 0L;
            }
            int maxSortOfChildren = getMaxSortOfChildren(parentId);
            authority.setSort(maxSortOfChildren + 1);
        }
        int insert = authorityMapper.insert(authority);
        if(!CollectionUtils.isEmpty(authorityAddDTO.getUrls())){
            addAuthorityUrl(authority.getId(), authorityAddDTO.getUrls());
        }
        return insert > 0 ? authority.getId() : null;
    }

    @Override
    @Transactional
    public Boolean updateAuthority(AuthorityUpdateDTO authorityUpdateDTO, Boolean isFullUpdate) {
        Authority authority = authorityMapper.selectById(authorityUpdateDTO.getId());
        if (authority == null || !authority.getType().equals(AuthorityType.ACTION)) {
            throw new BusinessException("该操作权限不存在");
        }
        if (isFullUpdate){
            AuthorityMapping.INSTANCE.overwriteAuthority(authorityUpdateDTO,authority);
            addAuthorityUrl(authority.getId(), authorityUpdateDTO.getUrls());
        } else {
            AuthorityMapping.INSTANCE.updateAuthority(authorityUpdateDTO,authority);
            if(!CollectionUtils.isEmpty(authorityUpdateDTO.getUrls())){
                addAuthorityUrl(authority.getId(), authorityUpdateDTO.getUrls());
            }
        }
        if(authorityUpdateDTO.getParentId() != null && !authorityUpdateDTO.getParentId().isEmpty() && !authorityUpdateDTO.getParentId().equals(authority.getParentId().toString())){
            Authority selectAuthority = authorityMapper.selectById(authorityUpdateDTO.getParentId());
            authority.setRootId(selectAuthority.getRootId());
        }
        return authorityMapper.updateById(authority) > 0;
    }

    @Override
    public AuthorityVO details(Long id) {
        Authority authority = authorityMapper.selectById(id);
        AuthorityVO authorityVO = AuthorityMapping.INSTANCE.toAuthorityVO(authority);
        List<AuthorityUrlDTO> urlDTOList = authorityUrlService.findAuthorityUrl(id);
        authorityVO.setUrls(urlDTOList);
        return authorityVO;
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

        return tree(AuthorityDomain.GLOBAL);
    }

    @Override
    public List<AuthorityVO> tree(AuthorityDomain domain) {
        QueryWrapper<Authority> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .lambda()
                .select(Authority::getId, Authority::getParentId, Authority::getName)
                .in(Authority::getType, AuthorityType.MENU, AuthorityType.ACTION)
                .eq(Authority::getAccessControl,AuthorityAccessControl.PROTECTED.name())
                .eq(Authority::getDomain,domain.getCode())
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

    public void addAuthorityUrl(Long id, List<AuthorityUrlDTO> authorityUrlDTOList) {
        // 清空
        authorityUrlService.lambdaUpdate()
                .eq(AuthorityUrl::getAuthorityId, id)
                .remove();
        if(CollectionUtils.isEmpty(authorityUrlDTOList)){
            return;
        }
        List<AuthorityUrl> authorityUrls = new ArrayList<>();
        for (AuthorityUrlDTO authorityUrlDTO : authorityUrlDTOList){
            AuthorityUrl authorityUrl = AuthorityUrlMapping.INSTANCE.toAuthorityUrl(authorityUrlDTO);
            authorityUrl.setId(IdGen.genId());
            authorityUrl.setAuthorityId(id);
            authorityUrls.add(authorityUrl);
        }
        authorityUrlService.saveBatch(authorityUrls);
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


    @Override
    public List<AuthorityVO> findByUserId(Long userId, AuthorityDomain domain) {
        List<AuthorityVO> authorityVOS = selfProvider.getObject().findByUserId(userId);
        return authorityVOS.stream()
                .filter(item -> item.getDomain().equals(domain))
                .collect(Collectors.toList());
    }

    // 根据用户ID查询权限
    @Override
    @Cacheable(value = "user:authority", key = "#p0")
    public List<AuthorityVO> findByUserId(Long userId) {
        if(userId == null){
            return Collections.emptyList();
        }
        List<RoleVO> roles = roleService.findByUserId(userId);
        LambdaQueryWrapper<Authority> wrapper = Wrappers.lambdaQuery();
        wrapper.select(
                Authority::getId,
                Authority::getParentId,
                Authority::getCode,
                Authority::getName,
                Authority::getType,
                Authority::getDomain
        );
        if (CollectionUtils.isEmpty(roles)) {
            wrapper.eq(
                    Authority::getAccessControl,
                    AuthorityAccessControl.AUTHENTICATED.name()
            );
        } else {
            List<Long> roleIds = roles.stream().map(RoleVO::getId).toList();
            List<Long> authorityIds = roleAuthorityService.findAuthorityIdByRoleId(roleIds);
            wrapper.and(w -> {
                // PUBLIC权限
                w.eq(Authority::getAccessControl, AuthorityAccessControl.AUTHENTICATED.name());
                // 角色授权权限
                if (!CollectionUtils.isEmpty(authorityIds)) {
                    w.or().in(Authority::getId, authorityIds);
                }
            });

        }
        List<Authority> authorities = authorityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(authorities)){
            return Collections.emptyList();
        }
        Set<Long> ids = authorities.stream().map(Authority::getId).collect(Collectors.toSet());
        List<AuthorityUrlDTO> urlDTOList = authorityUrlService.findAuthorityUrl(ids);
        if(CollectionUtils.isEmpty(urlDTOList)){
            return AuthorityMapping.INSTANCE.toAuthorityVO(authorities);
        }
        List<AuthorityVO> result = new ArrayList<>();
        Map<Long, List<AuthorityUrlDTO>> urlMap = urlDTOList.stream().collect(Collectors.groupingBy(AuthorityUrlDTO::getAuthorityId));
        for (Authority authority : authorities){
            AuthorityVO authorityVO = AuthorityMapping.INSTANCE.toAuthorityVO(authority);
            result.add(authorityVO);
            List<AuthorityUrlDTO> authorityUrlDTOS = urlMap.get(authority.getId());
            if(CollectionUtils.isEmpty(authorityUrlDTOS)){
                continue;
            }
            authorityVO.setUrls(authorityUrlDTOS);
        }
        return result;
    }

}
