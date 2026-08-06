package com.atlas.auth.service;

import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

public interface OAuth2ClientApplicationService extends IService<OAuth2ClientApplication> {


    OAuth2ClientApplication loadClientByRegisteredClientId(String registeredClientId);

    OAuth2ClientApplication loadClientByClientId(String registeredClientId);

    OAuth2ClientApplication findByRegisteredClientId(String registeredClientId);

    OAuth2ClientApplication findByClientId(String clientId);

    List<OAuth2ClientApplication> findByProjectId(Long projectId);

    default List<String> findRegisteredClientIdByProjectId(Long projectId){
        List<OAuth2ClientApplication> registeredClientIds = this.findByProjectId(projectId);
        if(CollectionUtils.isEmpty(registeredClientIds)){
            return Collections.emptyList();
        }
        return registeredClientIds.stream().map(OAuth2ClientApplication::getRegisteredClientId).toList();
    }

}
