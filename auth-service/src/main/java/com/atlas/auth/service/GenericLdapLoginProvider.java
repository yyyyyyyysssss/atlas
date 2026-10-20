package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.auth.config.exception.ThirdPartyAuthException;
import com.atlas.auth.config.security.ldap.LdapTemplateFactory;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapEncoder;

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
        SsoProviderProtocol protocol = SsoProviderProtocol.LDAP;
        String username = ldapLoginRequest.username();
        String password = ldapLoginRequest.password();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw ThirdPartyAuthException.invalidParameter(provider, protocol);
        }

        LdapProviderSettings settings = ssoProviderService.getSettings(provider, protocol);
        if (settings == null) {
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_NOT_FOUND, provider, protocol);
        }
        LdapTemplate ldapTemplate = ldapTemplateFactory.getAuthenticationTemplate(provider, settings);
        LdapTemplate queryTemplate = ldapTemplateFactory.getQueryTemplate(provider, settings);
        LdapUserInfo ldapUserInfo;
        try {
            LdapProviderSettings.UserSearch userSearch = settings.userSearch();
            if (userSearch == null || userSearch.filter() == null || userSearch.filter().isBlank() || !userSearch.filter().contains("{0}")) {
                throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_CONFIG_ERROR, provider, protocol);
            }
            String filter = userSearch.filter().replace("{0}", LdapEncoder.filterEncode(username));
            // 执行认证
            boolean authenticated = ldapTemplate.authenticate("", filter, password);
            if (!authenticated) {
                throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_AUTH_FAILED, provider, protocol);
            }
            // 查询用户信息
            LdapQuery query = LdapQueryBuilder.query()
                    .filter(filter);
            List<DirContextAdapter> users = queryTemplate
                    .search(query, (ContextMapper<DirContextAdapter>) ctx -> (DirContextAdapter) ctx);
            if (users.isEmpty()) {
                throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_AUTH_FAILED, provider, protocol, "LDAP user not found", null);
            }
            if (users.size() > 1) {
                throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_AUTH_FAILED, provider, protocol, "LDAP user search returned multiple results", null);
            }
            Attributes attributes = users.getFirst().getAttributes();
            ldapUserInfo = LdapUserInfo.fromAttributes(attributes, settings.mappings());
            ldapUserInfo.setProvider(provider);

        } catch (CommunicationException e) {
            log.error("LDAP communication failed, provider={}", provider, e);
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_UNAVAILABLE, provider, protocol, e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("LDAP user attribute mapping failed, provider={}", provider, e);
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_CONFIG_ERROR, provider, protocol, "LDAP user attribute mapping failed", e);
        } catch (AuthenticationException e) {
            log.warn("LDAP authentication failed, provider={}", provider, e);
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_AUTH_FAILED, provider, protocol);
        }
        return dispatchFederatedIdentity(ldapUserInfo);
    }

}
