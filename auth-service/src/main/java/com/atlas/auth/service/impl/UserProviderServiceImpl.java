package com.atlas.auth.service.impl;

import com.atlas.auth.domain.dto.UserProviderDTO;
import com.atlas.auth.domain.entity.UserProvider;
import com.atlas.auth.mapper.UserProviderMapper;
import com.atlas.auth.mapping.UserProviderMapping;
import com.atlas.auth.service.UserProviderService;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;


/**
 * (UserProvider)表服务实现类
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
@Service
@AllArgsConstructor
@Slf4j
public class UserProviderServiceImpl extends ServiceImpl<UserProviderMapper, UserProvider> implements UserProviderService {
    
    private UserProviderMapper userProviderMapper;


    @Override
    public UserProviderDTO getByProvider(String identityType, String identifier) {
        UserProvider entity = this.userProviderMapper.selectOne(new LambdaQueryWrapper<UserProvider>()
                .eq(UserProvider::getProvider, identityType)
                .eq(UserProvider::getProviderUserId, identifier));

        // 如果查询结果为空，直接返回 null
        if (entity == null) {
            return null;
        }

        return UserProviderMapping.INSTANCE.toUserProviderDTO(entity);
    }

    @Override
    @Transactional
    public void addUserProvider(Long userId, String provider, String providerUserId, Map<String, Object> extraInfo) {
        if(!StringUtils.hasText(provider) || !StringUtils.hasText(providerUserId)){
            throw new BusinessException("provider or providerUserId is empty");
        }
        UserProvider entity = this.userProviderMapper.selectOne(new LambdaQueryWrapper<UserProvider>()
                .eq(UserProvider::getProvider, provider)
                .eq(UserProvider::getProviderUserId, providerUserId));
        if (entity != null) {
            if(!entity.getUserId().equals(userId)){
                log.warn("身份标识 {} 已被用户 {} 占用，当前尝试绑定到用户 {}", entity.getProviderUserId(), entity.getUserId(), userId);
                throw new BusinessException("该社交账号已被其他用户绑定");
            }
            // 已经绑定过了，直接返回，不要再 save
            return;
        }
        UserProvider userIdentity = UserProvider
                .builder()
                .providerUserId(providerUserId)
                .provider(provider)
                .userId(userId)
                .extraInfo(extraInfo)
                .build();
        userIdentity.setId(IdGen.genId());
        this.save(userIdentity);
    }

    @Override
    public List<UserProvider> listByUserId(Long userId) {
        return userProviderMapper.selectList(new LambdaQueryWrapper<UserProvider>().eq(UserProvider::getUserId,userId));
    }
}

