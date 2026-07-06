package com.atlas.auth.service;

import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OAuth2ClientApplicationService extends IService<OAuth2ClientApplication> {


    OAuth2ClientApplication getByRegisteredClientId(String registeredClientId);

}
