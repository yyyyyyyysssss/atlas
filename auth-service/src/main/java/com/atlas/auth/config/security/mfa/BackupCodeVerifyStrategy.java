package com.atlas.auth.config.security.mfa;

import com.atlas.auth.service.UserMfaBackupCodeService;
import com.atlas.security.model.MfaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/3 14:53
 */
@Component
public class BackupCodeVerifyStrategy implements MfaVerifyStrategy{

    private final UserMfaBackupCodeService userMfaBackupCodeService;

    public BackupCodeVerifyStrategy(UserMfaBackupCodeService userMfaBackupCodeService) {
        this.userMfaBackupCodeService = userMfaBackupCodeService;
    }

    @Override
    public void verify(MfaTicketContext mfaTicketContext, String code) {
        Long userId = mfaTicketContext.getUserId();
        boolean valid = userMfaBackupCodeService.verifyAndConsume(userId, code);
        if (!valid) {
            throw new BadCredentialsException("备份码无效或已被使用");
        }
    }

    @Override
    public MfaType getMfaType() {

        return MfaType.BACKUP_CODE;
    }
}
