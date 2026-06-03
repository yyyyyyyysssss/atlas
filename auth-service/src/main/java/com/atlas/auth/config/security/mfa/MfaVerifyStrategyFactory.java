package com.atlas.auth.config.security.mfa;

import com.atlas.security.model.MfaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/3 14:54
 */
@Component
public class MfaVerifyStrategyFactory {

    private final Map<MfaType, MfaVerifyStrategy> strategies = new ConcurrentHashMap<>();

    // Spring 会自动将所有实现了 MfaVerifyStrategy 接口的 Bean 注入到这个 List 中
    public MfaVerifyStrategyFactory(List<MfaVerifyStrategy> strategyList) {
        for (MfaVerifyStrategy strategy : strategyList) {
            strategies.put(strategy.getMfaType(), strategy);
        }
    }

    /**
     * 根据 MfaType 获取对应的校验策略
     */
    public MfaVerifyStrategy getStrategy(MfaType mfaType) {
        MfaVerifyStrategy strategy = strategies.get(mfaType);
        if (strategy == null) {
            throw new BadCredentialsException("不支持的 MFA 验证类型: " + mfaType);
        }
        return strategy;
    }

}
