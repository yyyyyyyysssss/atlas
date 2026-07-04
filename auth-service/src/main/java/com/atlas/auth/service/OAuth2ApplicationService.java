package com.atlas.auth.service;

import com.atlas.auth.domain.entity.OAuth2Application;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OAuth2ApplicationService extends IService<OAuth2Application> {


    OAuth2Application getByRegisteredClientId(String registeredClientId);

}
