package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.auth.domain.entity.SsoProviderSettings;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.mapper.SsoProviderMapper;
import com.atlas.auth.service.SsoProviderService;
import com.atlas.auth.service.SsoProviderSettingsService;
import com.atlas.common.core.utils.JsonUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;


/**
 * (SsoProvider)表服务实现类
 *
 * @author ys
 * @since 2026-06-17 09:06:51
 */
@Service("ssoProviderService")
@RequiredArgsConstructor
@Slf4j
public class SsoProviderServiceImpl extends ServiceImpl<SsoProviderMapper, SsoProvider> implements SsoProviderService {
    
    private final SsoProviderMapper ssoProviderMapper;

    private final SsoProviderSettingsService ssoProviderSettingsService;

    @Override
    public SsoProvider getProvider(String provider) {
        return this.getById(provider);
    }

    @Override
    public <T> T getSettings(String provider, SsoProviderProtocol protocol, Class<T> clazz) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(protocol);
        SsoProviderSettings ssoProviderSettings = ssoProviderSettingsService.lambdaQuery()
                .eq(SsoProviderSettings::getProvider, provider)
                .eq(SsoProviderSettings::getProtocol, protocol)
                .one();
        if(ssoProviderSettings == null || ssoProviderSettings.getSettings() == null){
            return null;
        }
        return JsonUtils.convert(ssoProviderSettings.getSettings(),clazz);
    }
}

