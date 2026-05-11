package com.atlas.auth.config.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/11 14:46
 */
public class DelegatingRegisteredClientRepository implements RegisteredClientRepository {

    private final RegisteredClientRepository delegate;

    public DelegatingRegisteredClientRepository(RegisteredClientRepository delegate){
        this.delegate = delegate;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        this.delegate.save(registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        RegisteredClient registeredClient = this.delegate.findById(id);
        return wrapIfNecessary(registeredClient);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        RegisteredClient registeredClient = this.delegate.findByClientId(clientId);
        return wrapIfNecessary(registeredClient);
    }

    private RegisteredClient wrapIfNecessary(RegisteredClient client) {
        if (client == null) {
            return null;
        }
        // 检查当前请求是否符合“受信任”或“免授权”的条件
        if (isTrustworthyRequest()) {
            return RegisteredClient.from(client)
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(false) // 核心包装逻辑：动态覆盖设置
                            .requireProofKey(true) // 开启 PKCE 校验
                            .build())
                    .build();
        }
        return client;
    }

    private boolean isTrustworthyRequest() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr == null) return false;

        HttpServletRequest request = attr.getRequest();
        // 扫码登录通常由我们内部 PC 端触发，带有特定参数或 Header
        return "qr".equals(request.getParameter("login_mode"));
    }
}
