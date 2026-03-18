package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.user.config.idwork.IdGen;
import com.atlas.user.domain.dto.RoleDataScopeDTO;
import com.atlas.user.domain.entity.RoleDataScope;
import com.atlas.user.mapper.RoleDataScopeMapper;
import com.atlas.user.mapping.RoleDataScopeMapping;
import com.atlas.user.service.RoleDataScopeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * (RoleDataScope)表服务实现类
 *
 * @author ys
 * @since 2026-03-17 13:31:51
 */
@Service("roleDataScopeService")
@AllArgsConstructor
@Slf4j
public class RoleDataScopeServiceImpl extends ServiceImpl<RoleDataScopeMapper, RoleDataScope> implements RoleDataScopeService {
    
    private RoleDataScopeMapper roleDataScopeMapper;
    
    @Override
    @Transactional
    public void addRoleDataScope(Long roleId, List<Long> orgIds) {
        // 先删除
        deleteRoleDataScope(roleId);
        if(CollectionUtils.isEmpty(orgIds)){
            return;
        }
        // 再新增
        List<RoleDataScope> RoleDataScopeList = orgIds.stream()
                .map(m -> {
                    RoleDataScope roleDataScope = new RoleDataScope(roleId, m);
                    roleDataScope.setId(IdGen.genId());
                    return roleDataScope;
                })
                .collect(Collectors.toList());
        this.saveBatch(RoleDataScopeList);
    }

    @Override
    @Transactional
    public void deleteRoleDataScope(Long roleId) {
        if(roleId == null){
            throw new BusinessException("角色id不能为空");
        }
        QueryWrapper<RoleDataScope> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .lambda()
                .eq(RoleDataScope::getRoleId,roleId);
        this.remove(queryWrapper);
    }

    @Override
    public List<RoleDataScopeDTO> findRoleDataScope(Long roleId) {
        if(roleId == null){
            throw new BusinessException("角色id不能为空");
        }
        return findRoleDataScope(Collections.singleton(roleId));
    }

    @Override
    public List<RoleDataScopeDTO> findRoleDataScope(Collection<Long> roleIds) {
        if(CollectionUtils.isEmpty(roleIds)){
            throw new BusinessException("角色id不能为空");
        }
        QueryWrapper<RoleDataScope> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .lambda()
                .in(RoleDataScope::getRoleId,roleIds);
        List<RoleDataScope> RoleDataScopeList = roleDataScopeMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(RoleDataScopeList)){
            return Collections.emptyList();
        }
        return RoleDataScopeMapping.INSTANCE.toRoleDataScopeDTO(RoleDataScopeList);
    }
    
}

