package com.atlas.auth.config.security.mfa;

import com.atlas.auth.service.UserTotpBackupCodeService;
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

    private final UserTotpBackupCodeService userTotpBackupCodeService;

    public BackupCodeVerifyStrategy(UserTotpBackupCodeService userTotpBackupCodeService) {
        this.userTotpBackupCodeService = userTotpBackupCodeService;
    }

    @Override
    public void verify(MfaTicketContext mfaTicketContext, String code) {
        Long userId = mfaTicketContext.getUserId();
        boolean valid = userTotpBackupCodeService.verifyAndConsume(userId, code);
        if (!valid) {
            throw new BadCredentialsException("备份码无效或已被使用");
        }
    }

    @Override
    public MfaType getMfaType() {

        return MfaType.BACKUP_CODE;
    }
}
