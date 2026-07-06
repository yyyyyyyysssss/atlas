package com.atlas.auth.service;


import com.atlas.auth.domain.entity.OAuth2ClientSecret;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * (Oauth2ClientSecret)表服务接口
 *
 * @author ys
 * @since 2026-07-06 15:09:26
 */
public interface OAuth2ClientSecretService extends IService<OAuth2ClientSecret> {


    List<OAuth2ClientSecret> listValidSecretsByRegisteredClientId(String registeredClientId);


    long countValidSecrets(String registeredClientId);


    boolean removeByRegisteredClientId(String registeredClientId);
}

