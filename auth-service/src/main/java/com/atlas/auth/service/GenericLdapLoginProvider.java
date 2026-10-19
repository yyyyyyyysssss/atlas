package com.atlas.auth.service;

import com.atlas.auth.config.security.ldap.LdapTemplateFactory;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;

import javax.naming.directory.Attributes;
import javax.ws.rs.NotSupportedException;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/16 15:08
 */

@Slf4j
@RequiredArgsConstructor
public class GenericLdapLoginProvider extends AbstractThirdPartyLoginProvider {

    private final String providerName;

    private final LdapTemplateFactory ldapTemplateFactory;

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public SsoProviderProtocol protocol() {
        return SsoProviderProtocol.LDAP;
    }

    @Override
    public SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthRequestContext requestContext, Map<String, String> extraParams) {
        throw new NotSupportedException("LDAP does not support authorize URL");
    }

    @Override
    public ThirdPartyCallbackVO authenticate(ThirdPartyLoginRequest request) {
        LdapLoginRequest ldapLoginRequest = (LdapLoginRequest) request;
        String provider = getProviderName();
        String username = ldapLoginRequest.username();
        String password = ldapLoginRequest.password();
        if (username == null || username.isBlank()) {
            throw new BusinessException("LDAP username must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException("LDAP password must not be blank");
        }
        LdapProviderSettings settings = ssoProviderService.getSettings(provider, SsoProviderProtocol.LDAP);
        if (settings == null) {
            throw new BusinessException("LDAP Provider configuration not found: " + provider);
        }
        LdapTemplate ldapTemplate = ldapTemplateFactory.getAuthenticationTemplate(provider, settings);
        try {
            LdapProviderSettings.UserSearch userSearch = settings.userSearch();
            String filter = userSearch.filter().replace("{0}", username);
            boolean authenticated = ldapTemplate.authenticate("", filter, password);
            if (!authenticated) {
                throw new BusinessException("LDAP 用户名或密码错误");
            }

            LdapQuery query = LdapQueryBuilder.query()
                    .filter(filter);
            List<DirContextAdapter> users = ldapTemplateFactory.getQueryTemplate(provider, settings)
                    .search(query, (ContextMapper<DirContextAdapter>) ctx -> (DirContextAdapter) ctx);
            if (users.isEmpty()) {
                throw new BusinessException("LDAP 用户不存在");
            }
            if (users.size() > 1) {
                throw new BusinessException("LDAP 用户不唯一，拒绝登录");
            }
            Attributes attributes = users.getFirst().getAttributes();
            LdapUserInfo ldapUserInfo = LdapUserInfo.fromAttributes(attributes, settings.mappings());
            ldapUserInfo.setProvider(provider);

            return dispatchFederatedIdentity(ldapUserInfo);
        } catch (Exception e) {
            log.error("LDAP authenticate error: ", e);
            throw new BusinessException("LDAP authentication failed");
        }
    }

}
