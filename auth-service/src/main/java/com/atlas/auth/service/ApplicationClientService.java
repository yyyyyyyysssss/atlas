package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ApplicationClientSaveDTO;
import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.enums.ClientProtocol;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

import java.util.Collection;
import java.util.List;

public interface ApplicationClientService extends IService<ApplicationClient> {

    ApplicationClient saveInfo(ApplicationClientSaveDTO saveDTO);

    PageInfo<ApplicationClient> getPage(Long applicationId, ClientProtocol protocol, Integer pageNum, Integer pageSize);

    ApplicationClient findByRegisteredClientId(String registeredClientId);

    ApplicationClient getActiveByRegisteredClientId(String registeredClientId, ClientProtocol protocol);

    ApplicationClient findByClientId(String clientId);

    List<ApplicationClient> findByApplicationIds(Collection<Long> applicationIds);

}
