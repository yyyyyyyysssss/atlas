package com.atlas.auth.service;

import com.atlas.auth.domain.dto.CertificateInspectResponse;
import com.atlas.auth.domain.entity.UserCertificateCredentials;
import com.atlas.auth.domain.vo.UserCertificateVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserCertificateCredentialsService extends IService<UserCertificateCredentials>, AuthCredentialChecker {

    Long create(Long userId, String label, CertificateInspectResponse response);

    UserCertificateCredentials findByFingerprint(String fingerprint);

    UserCertificateCredentials findByPublicKeyFingerprint(String publicKeyFingerprint);

    List<UserCertificateVO> listByUserId(Long userId);

}
