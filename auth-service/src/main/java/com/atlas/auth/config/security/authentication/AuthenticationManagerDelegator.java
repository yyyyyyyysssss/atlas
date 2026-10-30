package com.atlas.auth.config.security.authentication;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * 多阶段认证管理器委托器。
 *
 * <p>包装 Spring Security 的 {@link ProviderManager}，在其完成一次认证处理后，
 * 根据返回的 {@link Authentication} 状态决定是否继续执行下一阶段认证。</p>
 *
 * <p>当返回的 Authentication 已认证时，直接返回认证结果；
 * 当尚未认证时，将当前结果作为下一阶段的输入，继续委托给 ProviderManager 处理。</p>
 *
 * <p>适用于需要多个 AuthenticationProvider 协同完成的多阶段认证流程。</p>
 *
 * @author ys
 * @since 2026/10/30
 */
public class AuthenticationManagerDelegator implements AuthenticationManager {

    private final AuthenticationManager delegate;

    public AuthenticationManagerDelegator(AuthenticationManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication current = authentication;
        while (true) {
            Authentication result = delegate.authenticate(current);
            if (result == null) {
                return null;
            }
            if (result.isAuthenticated()) {
                return result;
            }
            // 未认证成功，继续处理下一阶段
            current = result;
        }
    }
}
