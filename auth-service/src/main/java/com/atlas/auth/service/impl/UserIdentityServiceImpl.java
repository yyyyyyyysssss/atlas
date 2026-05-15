package com.atlas.auth.service.impl;

import com.atlas.auth.domain.dto.UserIdentityDTO;
import com.atlas.auth.domain.entity.UserIdentity;
import com.atlas.auth.mapper.UserIdentityMapper;
import com.atlas.auth.mapping.UserIdentityMapping;
import com.atlas.auth.service.UserIdentityService;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
    public void addUserIdentity(Long userId, ExternalIdentityDTO externalIdentityDTO) {
        if(externalIdentityDTO == null){
            return;
        }
        UserIdentity entity = this.baseMapper.selectOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getIdentityType, externalIdentityDTO.getProvider())
                .eq(UserIdentity::getIdentifier, externalIdentityDTO.getSub()));
        if (entity != null) {
            if(!entity.getUserId().equals(userId)){
                log.warn("身份标识 {} 已被用户 {} 占用，当前尝试绑定到用户 {}", entity.getIdentifier(), entity.getUserId(), userId);
                throw new BusinessException("该社交账号已被其他用户绑定");
            }
            // 已经绑定过了，直接返回，不要再 save
            return;
        }
        UserIdentity userIdentity = UserIdentity
                .builder()
                .identifier(externalIdentityDTO.getSub())
                .identityType(externalIdentityDTO.getProvider())
                .userId(userId)
                .extraInfo(externalIdentityDTO.getExtraInfo())
                .verified(true)
                .build();
        userIdentity.setId(IdGen.genId());
        this.save(userIdentity);
    }
    
}

