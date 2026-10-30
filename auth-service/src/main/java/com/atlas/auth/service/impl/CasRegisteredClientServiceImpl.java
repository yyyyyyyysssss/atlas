package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.domain.entity.CasRegisteredClient;
import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.mapper.CasRegisteredClientMapper;
import com.atlas.auth.service.ApplicationClientService;
import com.atlas.auth.service.CasRegisteredClientService;
import com.atlas.common.crypto.digest.DigestUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 *@Description 
 *@Author ys
 *@Date 2026/10/30 10:53
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CasRegisteredClientServiceImpl extends ServiceImpl<CasRegisteredClientMapper, CasRegisteredClient> implements CasRegisteredClientService {

    private final ApplicationClientService applicationClientService;

    @Override
    public CasRegisteredClient getEnabledByService(String service) {
        if (service == null || service.isBlank()) {
            return null;
        }
        String serviceHash = DigestUtils.sha256(service);
        ApplicationClient applicationClient = applicationClientService.findByClientId(serviceHash, ClientProtocol.CAS);
        if(applicationClient ==  null || !Boolean.TRUE.equals(applicationClient.getEnabled())){
            return null;
        }

        CasRegisteredClient registeredClient = this.getById(applicationClient.getRegisteredClientId());
        if (registeredClient == null) {
            return null;
        }

        // 再次确认原始 Service，避免只依赖 Hash
        if (!service.equals(registeredClient.getService())) {
            return null;
        }

        return registeredClient;
    }
}
