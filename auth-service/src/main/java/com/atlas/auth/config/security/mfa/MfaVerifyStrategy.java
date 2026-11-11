package com.atlas.auth.config.security.mfa;

import com.atlas.common.security.model.MfaType;
import com.atlas.common.security.token.AssuranceLevelAware;

public interface MfaVerifyStrategy extends AssuranceLevelAware {

    /**
     * 执行具体的 MFA 验证逻辑
     * @param mfaChallenge mfa 挑战
     * @param mfaCredential 用户输入的凭证
     */
    Long verify(MfaChallenge mfaChallenge, MfaCredential mfaCredential);

    /**
     * 声明该策略支持哪种 MFA 类型
     */
    MfaType getMfaType();

}
