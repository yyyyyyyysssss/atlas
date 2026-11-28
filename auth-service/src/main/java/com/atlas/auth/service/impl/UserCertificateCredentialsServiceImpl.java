package com.atlas.auth.service.impl;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.CertificateInspectResponse;
import com.atlas.auth.domain.entity.UserCertificateCredentials;
import com.atlas.auth.domain.enums.CertificateStatus;
import com.atlas.auth.domain.vo.UserCertificateVO;
import com.atlas.auth.enums.CredentialType;
import com.atlas.auth.mapper.UserCertificateCredentialsMapper;
import com.atlas.auth.service.UserCertificateCredentialsService;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/19 13:56
 */
@Slf4j
@Service("userCertificateCredentialsService")
@AllArgsConstructor
public class UserCertificateCredentialsServiceImpl extends ServiceImpl<UserCertificateCredentialsMapper, UserCertificateCredentials> implements UserCertificateCredentialsService {


    public Long create(Long userId, String label,  CertificateInspectResponse response) {
        Objects.requireNonNull(userId, "用户id不能为空");
        Objects.requireNonNull(response, "证书信息不能为空");
        if(!StringUtils.hasText(label)){
            label = "我的数字证书-" + LocalDate.now();
        }
        UserCertificateCredentials credentials = UserCertificateCredentials
                .builder()
                .userId(userId)
                .label(label)
                .fingerprint(response.fingerprint())
                .publicKeyFingerprint(response.publicKeyFingerprint())
                .serialNumber(response.serialNumber())
                .issuer(response.issuer())
                .subject(response.subject())
                .notBefore(response.notBefore().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .notAfter(response.notAfter().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .enabled(Boolean.TRUE)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        credentials.setId(IdGen.genId());
        try {
            this.save(credentials);
        } catch (DuplicateKeyException e) {
            throw new AuthServiceException("证书已存在，请勿重复绑定");
        }
        return credentials.getId();
    }

    @Override
    public UserCertificateCredentials findByFingerprint(String fingerprint) {
        if (!StringUtils.hasText(fingerprint)) {
            return null;
        }
        return this.lambdaQuery()
                .eq(UserCertificateCredentials::getFingerprint, fingerprint)
                .one();
    }

    @Override
    public UserCertificateCredentials findByPublicKeyFingerprint(String publicKeyFingerprint) {
        if (!StringUtils.hasText(publicKeyFingerprint)) {
            return null;
        }
        return this.lambdaQuery()
                .eq(UserCertificateCredentials::getPublicKeyFingerprint, publicKeyFingerprint)
                .one();
    }

    @Override
    public List<UserCertificateVO> listByUserId(Long userId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        return this.lambdaQuery()
                .eq(UserCertificateCredentials::getUserId, userId)
                .list()
                .stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public CredentialType getCredentialType() {
        return CredentialType.CERTIFICATE;
    }

    @Override
    public boolean hasCredential(Long userId) {
        Objects.requireNonNull(userId, "用户id不能为空");

        return this.lambdaQuery()
                .eq(UserCertificateCredentials::getUserId, userId)
                .eq(UserCertificateCredentials::getEnabled, Boolean.TRUE)
                .isNull(UserCertificateCredentials::getRevokedTime)
                .exists();
    }

    @Override
    public boolean hasCredentialExcluding(Long userId, Object credentialId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        Objects.requireNonNull(credentialId, "凭证id不能为空");
        try {
            Long targetId = Long.valueOf(String.valueOf(credentialId));

            return this.lambdaQuery()
                    .eq(UserCertificateCredentials::getUserId, userId)
                    .eq(UserCertificateCredentials::getEnabled, Boolean.TRUE)
                    .isNull(UserCertificateCredentials::getRevokedTime)
                    .ne(UserCertificateCredentials::getId, targetId)
                    .exists();
        } catch (NumberFormatException e) {
            log.error("【证书检查】解析凭证ID失败, userId: {}, credentialId: {}", userId, credentialId, e);
            return false;
        }
    }


    private UserCertificateVO convertToVO(UserCertificateCredentials entity) {
        LocalDateTime now = LocalDateTime.now();

        return UserCertificateVO.builder()
                .id(entity.getId())
                .label(entity.getLabel())
                .notAfter(entity.getNotAfter())
                .expireDays(calculateExpireDays(entity.getNotAfter()))
                .status(calculateStatus(entity, now))
                .createTime(entity.getCreateTime())
                .build();
    }

    private CertificateStatus calculateStatus(UserCertificateCredentials entity, LocalDateTime now) {

        if (entity.getRevokedTime() != null) {
            return CertificateStatus.REVOKED;
        }

        if (!entity.getEnabled()) {
            return CertificateStatus.DISABLED;
        }

        if (!entity.getNotAfter().isAfter(now)) {
            return CertificateStatus.EXPIRED;
        }

        if (!entity.getNotAfter().isAfter(now.plusDays(30))) {
            return CertificateStatus.EXPIRING;
        }

        return CertificateStatus.VALID;
    }


    private Integer calculateExpireDays(LocalDateTime notAfter) {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), notAfter.toLocalDate());
    }
}
