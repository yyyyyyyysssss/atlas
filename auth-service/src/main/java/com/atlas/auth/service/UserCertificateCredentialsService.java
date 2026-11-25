package com.atlas.auth.service;

import com.atlas.auth.domain.entity.UserCertificateCredentials;
import com.atlas.auth.domain.vo.UserCertificateVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserCertificateCredentialsService extends IService<UserCertificateCredentials>, AuthCredentialChecker{

    UserCertificateCredentials findByFingerprint(String fingerprint);

    List<UserCertificateVO> listByUserId(Long userId);

}
