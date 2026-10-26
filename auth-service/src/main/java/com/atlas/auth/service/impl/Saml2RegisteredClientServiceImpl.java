package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.mapper.Saml2RegisteredClientMapper;
import com.atlas.auth.service.ApplicationClientService;
import com.atlas.auth.service.Saml2RegisteredClientService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/10 9:30
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Saml2RegisteredClientServiceImpl extends ServiceImpl<Saml2RegisteredClientMapper, Saml2RegisteredClient> implements Saml2RegisteredClientService {

    private final ApplicationClientService applicationClientService;

    @Override
    public Saml2RegisteredClient getEnabledByEntityId(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        ApplicationClient applicationClient = applicationClientService.findByClientId(entityId, ClientProtocol.SAML2);
        if(applicationClient ==  null || !applicationClient.getEnabled()){
            return null;
        }

        return this.getById(applicationClient.getRegisteredClientId());
    }

}
