package com.atlas.auth.service;

import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.baomidou.mybatisplus.extension.service.IService;

public interface Saml2RegisteredClientService extends IService<Saml2RegisteredClient> {

    Saml2RegisteredClient getEnabledByEntityId(String entityId);

}
