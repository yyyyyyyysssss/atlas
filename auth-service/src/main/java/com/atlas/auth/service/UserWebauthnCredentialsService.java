package com.atlas.auth.service;

import com.atlas.auth.domain.entity.UserWebauthnCredentials;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

import java.util.List;

public interface UserWebauthnCredentialsService extends IService<UserWebauthnCredentials>, UserCredentialRepository, AuthCredentialChecker {

    List<UserWebauthnCredentials> listByUserId(Long userId);

}
