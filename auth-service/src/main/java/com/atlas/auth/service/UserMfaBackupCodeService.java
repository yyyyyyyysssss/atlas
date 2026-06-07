package com.atlas.auth.service;


import com.atlas.auth.domain.entity.UserMfaBackupCode;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * (UserMfaBackupCode)表服务接口
 *
 * @author ys
 * @since 2026-05-28 14:22:19
 */
public interface UserMfaBackupCodeService extends IService<UserMfaBackupCode> {

    List<String> refreshBackupCodes(Long userId);

    boolean verifyAndConsume(Long userId, String inputCode);

    int countRemainingCodes(Long userId);

    boolean hasActiveCodes(Long userId);

    boolean removeByUserId(Long userId);
}

