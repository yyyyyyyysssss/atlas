package com.atlas.auth.service;

import com.atlas.auth.domain.entity.CasRegisteredClient;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CasRegisteredClientService extends IService<CasRegisteredClient> {

    /**
     * 根据 CAS Service 查询已启用的客户端。
     */
    CasRegisteredClient getEnabledByService(String service);

}
