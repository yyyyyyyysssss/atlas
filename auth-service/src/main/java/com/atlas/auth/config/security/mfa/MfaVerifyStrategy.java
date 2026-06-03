package com.atlas.auth.config.security.mfa;

import com.atlas.security.model.MfaType;

public interface MfaVerifyStrategy {

    /**
     * 执行具体的 MFA 验证逻辑
     * @param mfaTicketContext ticket 上下文（内含 userId 等信息）
     * @param code 用户输入的验证码/凭证
     */
    void verify(MfaTicketContext mfaTicketContext, String code);

    /**
     * 声明该策略支持哪种 MFA 类型
     */
    MfaType getMfaType();

}
