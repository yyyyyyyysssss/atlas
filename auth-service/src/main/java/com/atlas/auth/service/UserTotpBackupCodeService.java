package com.atlas.auth.service;


import com.atlas.auth.domain.entity.UserTotpBackupCode;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * (UserTotpBackupCode)表服务接口
 *
 * @author ys
 * @since 2026-05-28 14:22:19
 */
public interface UserTotpBackupCodeService extends IService<UserTotpBackupCode> {

    List<String> refreshBackupCodes(Long userId);

    boolean verifyAndConsume(Long userId, String inputCode);

    int countRemainingCodes(Long userId);

    boolean removeByUserId(Long userId);
}

