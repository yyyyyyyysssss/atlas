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
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attr != null){
            HttpServletRequest request = attr.getRequest();
            String prompt = request.getParameter("prompt");
            // 只有当明确携带 prompt=consent 时，才强制要求授权确认
            if ("consent".equals(prompt)) {
                return RegisteredClient.from(client)
                        .clientSettings(ClientSettings.builder()
                                .requireAuthorizationConsent(true)
                                .requireProofKey(client.getClientSettings().isRequireProofKey()) // 开启 PKCE 校验
                                .build())
                        .build();
            }
        }

        return RegisteredClient.from(client)
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false) // 默认不弹窗
                        .requireProofKey(client.getClientSettings().isRequireProofKey())
                        .build())
                .build();
    }
}
