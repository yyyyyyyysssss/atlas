package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.OAuth2ClientSecret;
import com.atlas.auth.mapper.OAuth2ClientSecretMapper;
import com.atlas.auth.service.OAuth2ClientSecretService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


/**
 * (Oauth2ClientSecret)表服务实现类
 *
 * @author ys
 * @since 2026-07-06 15:09:26
 */
@Service("oauth2ClientSecretService")
@AllArgsConstructor
@Slf4j
public class OAuth2ClientSecretServiceImpl extends ServiceImpl<OAuth2ClientSecretMapper, OAuth2ClientSecret> implements OAuth2ClientSecretService {
    
    private OAuth2ClientSecretMapper oauth2ClientSecretMapper;


    @Override
    public List<OAuth2ClientSecret> listValidSecretsByRegisteredClientId(String registeredClientId) {
        return this.list(new LambdaQueryWrapper<OAuth2ClientSecret>()
                .eq(OAuth2ClientSecret::getRegisteredClientId, registeredClientId)
                .and(wrapper -> wrapper
                        .isNull(OAuth2ClientSecret::getClientSecretExpiresAt)
                        .or()
                        .gt(OAuth2ClientSecret::getClientSecretExpiresAt, LocalDateTime.now())
                )
                .orderByAsc(OAuth2ClientSecret::getCreateTime)
        );
    }

    @Override
    public long countValidSecrets(String registeredClientId) {
        return this.count(new LambdaQueryWrapper<OAuth2ClientSecret>()
                .eq(OAuth2ClientSecret::getRegisteredClientId, registeredClientId)
                .and(wrapper -> wrapper
                        .isNull(OAuth2ClientSecret::getClientSecretExpiresAt)
                        .or()
                        .gt(OAuth2ClientSecret::getClientSecretExpiresAt, LocalDateTime.now())
                )
        );
    }

    @Override
    public boolean removeByRegisteredClientId(String registeredClientId) {
        return this.remove(new LambdaQueryWrapper<OAuth2ClientSecret>()
                .eq(OAuth2ClientSecret::getRegisteredClientId, registeredClientId)
        );
    }
    
}

