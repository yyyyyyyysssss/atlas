package com.atlas.auth.service;


import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.auth.domain.entity.SsoProviderSettings;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * (SsoProvider)表服务接口
 *
 * @author ys
 * @since 2026-06-17 09:06:50
 */
public interface SsoProviderService extends IService<SsoProvider> {

    SsoProvider getProvider(String provider);

    <T> T getSettings(String provider, SsoProviderProtocol protocol, Class<T> clazz);

}

