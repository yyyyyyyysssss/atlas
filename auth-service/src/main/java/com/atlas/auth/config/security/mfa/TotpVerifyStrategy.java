package com.atlas.auth.config.security.mfa;

import com.atlas.auth.domain.entity.UserTotpCredentials;
import com.atlas.auth.service.TotpService;
import com.atlas.auth.service.UserTotpCredentialsService;
import com.atlas.security.model.MfaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/3 14:51
 */

@Component
public class TotpVerifyStrategy implements MfaVerifyStrategy{

    private final UserTotpCredentialsService userTotpCredentialsService;

    private final TotpService totpService;

    public TotpVerifyStrategy(UserTotpCredentialsService userTotpCredentialsService, TotpService totpService) {
        this.userTotpCredentialsService = userTotpCredentialsService;
        this.totpService = totpService;
    }

    @Override
    public void verify(MfaTicketContext mfaTicketContext, String code) {
        Long userId = mfaTicketContext.getUserId();
        // 获取用户绑定的 TOTP 密钥
        UserTotpCredentials userTotpCredentials = userTotpCredentialsService.getActivatedByUserId(userId);
        if(userTotpCredentials == null){
            throw new BadCredentialsException("验证码错误或已失效");
        }
        boolean verify;
        try {
            // 即使 totpService 接收 Integer，也在这里安全转换，防止非数字引发 500 崩溃
            verify = totpService.verify(userTotpCredentials.getSecretKey(), Integer.parseInt(code));
        } catch (NumberFormatException e) {
            verify = false;
        }
        if(!verify){
            throw new BadCredentialsException("验证码错误或已失效");
        }
    }

    @Override
    public MfaType getMfaType() {

        return MfaType.TOTP;
    }
}
