package com.atlas.auth.config.security.oauth2;

import com.atlas.auth.domain.entity.OAuth2ClientSecret;
import com.atlas.auth.service.OAuth2ClientSecretService;
import com.atlas.auth.service.ProjectService;
import com.atlas.security.encoder.MultiSecretPayload;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/11 14:46
 */
@Slf4j
public class DelegatingRegisteredClientRepository implements RegisteredClientRepository {

    private final RegisteredClientRepository delegate;

    private final OAuth2ClientSecretService oauth2ClientSecretService;

    private final ProjectService projectService;

    public DelegatingRegisteredClientRepository(RegisteredClientRepository delegate, OAuth2ClientSecretService oauth2ClientSecretService, ProjectService projectService){
        this.delegate = delegate;
        this.oauth2ClientSecretService = oauth2ClientSecretService;
        this.projectService = projectService;
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

    @SuppressWarnings("ConstantConditions")
    private RegisteredClient wrapIfNecessary(RegisteredClient client) {
        if (client == null) {
            return null;
        }
        if(!projectService.isProjectActiveByRegisteredClientId(client.getId())){
            log.warn("OAuth2 鉴权拦截：客户端 [{}] (RegisteredClientId: {}) 所属项目已被归档、停用或不存在",
                    client.getClientId(), client.getId());
            return null;
        }
        String combinedSecret;
        // 查出当前客户端有效的密钥
        List<OAuth2ClientSecret> activeSecrets = oauth2ClientSecretService.listValidSecretsByRegisteredClientId(client.getId());
        if(activeSecrets != null && !activeSecrets.isEmpty()){
            // 多密钥
            List<String> secrets = activeSecrets.stream().map(OAuth2ClientSecret::getClientSecret).toList();
            combinedSecret = MultiSecretPayload.encode(secrets.toArray(new String[0]));
        } else {
            // 历史兼容
            combinedSecret = client.getClientSecret();
        }
        // 检查当前请求是否符合“受信任”或“免授权”的条件
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 默认不弹窗
        boolean requireConsent = false;
        if(attr != null){
            HttpServletRequest request = attr.getRequest();
            String prompt = request.getParameter("prompt");
            // 只有当明确携带 prompt=consent 时，才强制要求授权确认
            if ("consent".equals(prompt)) {
                requireConsent = true;
            }
        }

        ClientSettings updatedSettings = ClientSettings.withSettings(client.getClientSettings().getSettings())
                .requireAuthorizationConsent(requireConsent)
                .build();
        return RegisteredClient.from(client)
                .clientSecret(combinedSecret)
                .clientSettings(updatedSettings)
                .build();
    }
}
