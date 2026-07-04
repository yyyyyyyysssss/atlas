package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.OAuth2Application;
import com.atlas.auth.mapper.OAuth2ApplicationMapper;
import com.atlas.auth.service.OAuth2ApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
@Slf4j
public class OAuth2ApplicationServiceImpl extends ServiceImpl<OAuth2ApplicationMapper, OAuth2Application> implements OAuth2ApplicationService {


    @Override
    public OAuth2Application getByRegisteredClientId(String registeredClientId) {
        return this.lambdaQuery()
                .eq(OAuth2Application::getRegisteredClientId, registeredClientId)
                .one();
    }



}
