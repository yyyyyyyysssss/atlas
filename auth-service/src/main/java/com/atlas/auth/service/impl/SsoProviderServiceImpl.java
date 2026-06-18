package com.atlas.auth.service.impl;

import com.atlas.auth.domain.dto.Decryptable;
import com.atlas.auth.domain.dto.SsoSettings;
import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.auth.domain.entity.SsoProviderSettings;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.mapper.SsoProviderMapper;
import com.atlas.auth.service.SsoProviderService;
import com.atlas.auth.service.SsoProviderSettingsService;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.utils.KeyManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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

    private final SsoProviderSettingsService ssoProviderSettingsService;

    @Override
    public SsoProvider getProvider(String provider) {
        return this.getById(provider);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SsoSettings> T getSettings(String provider, SsoProviderProtocol protocol) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(protocol);
        SsoProviderSettings ssoProviderSettings = ssoProviderSettingsService.lambdaQuery()
                .eq(SsoProviderSettings::getProvider, provider)
                .eq(SsoProviderSettings::getProtocol, protocol)
                .one();
        if(ssoProviderSettings == null || ssoProviderSettings.getSettings() == null){
            return null;
        }
        T t = (T)JsonUtils.convert(ssoProviderSettings.getSettings(), protocol.getSettingsClass());
        // 解密 clientSecret
        if(t instanceof Decryptable<?> d){
            t = (T) d.decrypt(KeyManager.deriveServiceKey(provider));
        }
        return t;
    }

    @Override
    public List<SsoProvider> listByProtocol(SsoProviderProtocol protocol) {
        List<SsoProviderSettings> settingsList = ssoProviderSettingsService.lambdaQuery()
                .select(SsoProviderSettings::getProvider)
                .eq(SsoProviderSettings::getProtocol, protocol)
                .list();

        if (settingsList.isEmpty()) {
            return List.of();
        }
        List<String> providers = settingsList.stream()
                .map(SsoProviderSettings::getProvider)
                .distinct()
                .toList();
        return this.lambdaQuery()
                .in(SsoProvider::getProvider, providers)
                .eq(SsoProvider::getEnabled, true)
                .list();
    }
}

