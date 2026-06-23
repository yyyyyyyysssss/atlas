package com.atlas.auth.service;


import com.atlas.auth.domain.entity.UserWeb3Credentials;
import com.atlas.auth.enums.Web3WalletType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Optional;

/**
 * (UserWeb3Credentials)表服务接口
 *
 * @author ys
 * @since 2026-06-09 16:45:48
 */
public interface UserWeb3CredentialsService extends IService<UserWeb3Credentials>, AuthCredentialChecker {

    Optional<UserWeb3Credentials> getByAddress(String address);

    List<UserWeb3Credentials> listByUserId(Long userId);

    boolean saveCredential(Long userId, String address, Web3WalletType walletType,String label, String source);

    boolean removeCredential(Long userId, String address);

}

