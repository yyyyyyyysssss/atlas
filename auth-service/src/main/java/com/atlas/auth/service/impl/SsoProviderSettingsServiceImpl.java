package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.SsoProviderSettings;
import com.atlas.auth.mapper.SsoProviderSettingsMapper;
import com.atlas.auth.service.SsoProviderSettingsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * (SsoProviderSettings)表服务实现类
 *
 * @author ys
 * @since 2026-06-17 09:08:23
 */
@Service("ssoProviderSettingsService")
@AllArgsConstructor
@Slf4j
public class SsoProviderSettingsServiceImpl extends ServiceImpl<SsoProviderSettingsMapper, SsoProviderSettings> implements SsoProviderSettingsService {
    
    private SsoProviderSettingsMapper ssoProviderSettingsMapper;
    
}

