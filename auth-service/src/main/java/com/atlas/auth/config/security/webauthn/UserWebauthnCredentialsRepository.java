package com.atlas.auth.config.security.webauthn;

import com.atlas.auth.domain.entity.UserWebauthnCredentials;
import com.atlas.auth.mapper.UserWebauthnCredentialsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserWebauthnCredentialsRepository implements UserCredentialRepository {

    private final UserWebauthnCredentialsMapper userWebauthnCredentialsMapper;

    public UserWebauthnCredentialsRepository(UserWebauthnCredentialsMapper userWebauthnCredentialsMapper) {
        this.userWebauthnCredentialsMapper = userWebauthnCredentialsMapper;
    }

    @Override
    public void delete(Bytes credentialId) {
        userWebauthnCredentialsMapper.deleteById(credentialId.toBase64UrlString());
    }

    @Override
    public void save(CredentialRecord credentialRecord) {
        UserWebauthnCredentials entity = toEntity(credentialRecord);
        int rows = userWebauthnCredentialsMapper.updateById(entity);
        if (rows == 0) {
            userWebauthnCredentialsMapper.insert(entity);
        }
    }

    @Override
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        UserWebauthnCredentials entity = userWebauthnCredentialsMapper.selectById(credentialId.toBase64UrlString());
        return entity == null ? null : toDomain(entity);
    }

    @Override
    public List<CredentialRecord> findByUserId(Bytes userId) {
        List<UserWebauthnCredentials> entities = userWebauthnCredentialsMapper.selectList(
                new LambdaQueryWrapper<UserWebauthnCredentials>()
                        .eq(UserWebauthnCredentials::getUserId, Long.parseLong(new String(userId.getBytes())))
        );
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private UserWebauthnCredentials toEntity(CredentialRecord record) {
        UserWebauthnCredentials entity = new UserWebauthnCredentials();
        entity.setCredentialId(record.getCredentialId().toBase64UrlString());
        entity.setUserId(Long.parseLong(new String(record.getUserEntityUserId().getBytes())));
        entity.setPublicKey(record.getPublicKey().getBytes());
        entity.setSignatureCount(record.getSignatureCount());
        entity.setUvInitialized(record.isUvInitialized());
        entity.setBackupEligible(record.isBackupEligible());
        entity.setPublicKeyCredentialType(record.getCredentialType() != null ? record.getCredentialType().getValue() : null);
        entity.setBackupState(record.isBackupState());
        entity.setAttestationObject(record.getAttestationObject() != null ? record.getAttestationObject().getBytes() : null);
        entity.setAttestationClientDataJson(record.getAttestationClientDataJSON() != null ? record.getAttestationClientDataJSON().getBytes() : null);
        entity.setCreateTime(LocalDateTime.ofInstant(record.getCreated(), ZoneId.systemDefault()));
        entity.setUpdateTime(LocalDateTime.ofInstant(record.getLastUsed(), ZoneId.systemDefault()));
        entity.setLabel(record.getLabel());

        if (record.getTransports() != null) {
            String transports = record.getTransports().stream()
                    .map(AuthenticatorTransport::getValue)
                    .collect(Collectors.joining(","));
            entity.setAuthenticatorTransports(transports);
        }
        return entity;
    }

    private CredentialRecord toDomain(UserWebauthnCredentials entity) {
        Set<AuthenticatorTransport> transports = new HashSet<>();
        if (entity.getAuthenticatorTransports() != null && !entity.getAuthenticatorTransports().isBlank()) {
            for (String t : entity.getAuthenticatorTransports().split(",")) {
                transports.add(AuthenticatorTransport.valueOf(t.trim().toUpperCase()));
            }
        }
        Long userId = entity.getUserId();
        byte[] userIdBytes = String.valueOf(userId).getBytes(StandardCharsets.UTF_8);

        return ImmutableCredentialRecord.builder()
                .credentialId(Bytes.fromBase64(entity.getCredentialId()))
                .userEntityUserId(new Bytes(userIdBytes))
                .publicKey(new ImmutablePublicKeyCose(entity.getPublicKey()))
                .signatureCount(entity.getSignatureCount())
                .uvInitialized(entity.getUvInitialized())
                .backupEligible(entity.getBackupEligible())
                .credentialType(entity.getPublicKeyCredentialType() != null ? PublicKeyCredentialType.valueOf(entity.getPublicKeyCredentialType()) : null)
                .backupState(entity.getBackupState())
                .attestationObject(entity.getAttestationObject() != null ? new Bytes(entity.getAttestationObject()) : null)
                .attestationClientDataJSON(entity.getAttestationClientDataJson() != null ? new Bytes(entity.getAttestationClientDataJson()) : null)
                .created(entity.getCreateTime().atZone(ZoneId.systemDefault()).toInstant())
                .lastUsed(entity.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant())
                .label(entity.getLabel())
                .transports(transports)
                .build();
    }

}
