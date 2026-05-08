package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.user.domain.dto.UserIdentityDTO;
import com.atlas.user.domain.entity.UserIdentity;
import com.atlas.user.mapper.UserIdentityMapper;
import com.atlas.user.mapping.UserIdentityMapping;
import com.atlas.user.service.UserIdentityService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 * (UserIdentity)表服务实现类
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
@Service("userIdentityService")
@AllArgsConstructor
@Slf4j
public class UserIdentityServiceImpl extends ServiceImpl<UserIdentityMapper, UserIdentity> implements UserIdentityService {
    
    private UserIdentityMapper userIdentityMapper;


    @Override
    public UserIdentityDTO getByIdentity(String identityType, String identifier) {
        UserIdentity entity = this.baseMapper.selectOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getIdentityType, identityType)
                .eq(UserIdentity::getIdentifier, identifier));

        // 如果查询结果为空，直接返回 null
        if (entity == null) {
            return null;
        }

        return UserIdentityMapping.INSTANCE.toUserIdentityDTO(entity);
    }

    @Override
    @Transactional
    public void addUserIdentity(Long masterId, List<UserIdentityDTO> list) {
        // 先删除
        deleteUserIdentity(masterId);
        if(CollectionUtils.isEmpty(list)){
            return;
        }
        // 再新增
        List<UserIdentity> UserIdentityList = list.stream()
                .map(UserIdentityMapping.INSTANCE::toUserIdentity)
                .peek(p -> {
                    p.setId(IdGen.genId());
                    p.setUserId(masterId);
                }).collect(Collectors.toList());
        this.saveBatch(UserIdentityList);
    }

    @Override
    @Transactional
    public void deleteUserIdentity(Long masterId) {
        if(masterId == null){
            throw new BusinessException("id不能为空");
        }
        QueryWrapper<UserIdentity> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .lambda()
                .eq(UserIdentity::getUserId,masterId);
        this.remove(queryWrapper);
    }

    @Override
    public List<UserIdentityDTO> findUserIdentity(Long masterId) {
        if(masterId == null){
            throw new BusinessException("伙伴id不能为空");
        }
        return findUserIdentity(Collections.singleton(masterId));
    }

    @Override
    public List<UserIdentityDTO> findUserIdentity(Collection<Long> masterIds) {
        if(CollectionUtils.isEmpty(masterIds)){
            throw new BusinessException("id不能为空");
        }
        QueryWrapper<UserIdentity> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .lambda()
                .in(UserIdentity::getUserId,masterIds);
        List<UserIdentity> UserIdentityList = userIdentityMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(UserIdentityList)){
            return Collections.emptyList();
        }
        return UserIdentityMapping.INSTANCE.toUserIdentityDTO(UserIdentityList);
    }
    
}

