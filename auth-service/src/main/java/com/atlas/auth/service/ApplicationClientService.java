package com.atlas.auth.service;

import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.enums.ClientProtocol;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface ApplicationClientService extends IService<ApplicationClient> {

    PageInfo<ApplicationClient> getPage(Long applicationId, ClientProtocol protocol, Integer pageNum, Integer pageSize);

    List<ApplicationClient> findByApplication(Long applicationId, ClientProtocol protocol);

    ApplicationClient findByRegisteredClientId(String registeredClientId);

    ApplicationClient findByClientId(String clientId);

}
