package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.UserWeb3Credentials;
import com.atlas.auth.enums.CredentialType;
import com.atlas.auth.enums.Web3WalletType;
import com.atlas.auth.mapper.UserWeb3CredentialsMapper;
import com.atlas.auth.service.AuthCredentialChecker;
import com.atlas.auth.service.UserWeb3CredentialsService;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * (UserWeb3Credentials)表服务实现类
 *
 * @author ys
 * @since 2026-06-09 16:45:48
 */
@Service("userWeb3CredentialsService")
@AllArgsConstructor
@Slf4j
public class UserWeb3CredentialsServiceImpl extends ServiceImpl<UserWeb3CredentialsMapper, UserWeb3Credentials> implements UserWeb3CredentialsService {

    @Override
    public Optional<UserWeb3Credentials> getByAddress(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        UserWeb3Credentials credential = this.lambdaQuery()
                .eq(UserWeb3Credentials::getAddress, address)
                .one();

        return Optional.ofNullable(credential);
    }

    @Override
    public List<UserWeb3Credentials> listByUserId(Long userId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        // 查询该用户绑定的所有凭证列表
        return this.lambdaQuery()
                .eq(UserWeb3Credentials::getUserId, userId)
                .list();
    }

    @Override
    public boolean saveCredential(Long userId, String address, Web3WalletType walletType, String label, String source) {
        Objects.requireNonNull(userId, "用户id不能为空");
        Objects.requireNonNull(address, "地址不能为空");
        UserWeb3Credentials credential = new UserWeb3Credentials();
        credential.setId(IdGen.genId());
        credential.setUserId(userId);
        credential.setAddress(address);
        credential.setWalletType(walletType);
        credential.setLabel(label);
        credential.setSource(source);

        boolean success = this.save(credential);
        if (success) {
            log.info("【Web3凭证操作】成功为用户 userId: {} 插入钱包地址: {}", userId, address);
        }
        return success;
    }

    @Override
    public boolean removeCredential(Long userId, String address) {
        Objects.requireNonNull(userId, "用户id不能为空");
        Objects.requireNonNull(address, "地址不能为空");
        boolean success = this.lambdaUpdate()
                .eq(UserWeb3Credentials::getUserId, userId)
                .eq(UserWeb3Credentials::getAddress, address)
                .remove();
        if (success) {
            log.warn("【Web3凭证操作】用户 userId: {} 成功解绑了钱包地址: {}", userId, address);
        }
        return success;
    }

    @Override
    public CredentialType getCredentialType() {
        return CredentialType.WEB3;
    }

    @Override
    public boolean hasCredential(Long userId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        Long count = this.lambdaQuery()
                .eq(UserWeb3Credentials::getUserId, userId)
                .count();

        return count != null && count > 0;
    }

    @Override
    public boolean hasCredentialExcluding(Long userId, Object credentialId) {
        Objects.requireNonNull(userId, "用户id不能为空");
        Objects.requireNonNull(credentialId, "凭证id不能为空");
        try {
            Long targetId = Long.valueOf(String.valueOf(credentialId));

            Long count = this.lambdaQuery()
                    .eq(UserWeb3Credentials::getUserId, userId)
                    .ne(UserWeb3Credentials::getId, targetId)
                    .count();

            return count != null && count > 0;
        }catch (NumberFormatException e){
            log.error("【Web3凭证检查】解析凭证ID失败, userId: {}, credentialId: {}", userId, credentialId, e);
            return false;
        }
    }
}

