package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.atlas.auth.mapper.OAuth2ApplicationMapper;
import com.atlas.auth.service.OAuth2ClientApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
@Slf4j
public class OAuth2ApplicationServiceImpl extends ServiceImpl<OAuth2ApplicationMapper, OAuth2ClientApplication> implements OAuth2ClientApplicationService {


    @Override
    public OAuth2ClientApplication getByRegisteredClientId(String registeredClientId) {
        return this.lambdaQuery()
                .eq(OAuth2ClientApplication::getRegisteredClientId, registeredClientId)
                .one();
    }



}
