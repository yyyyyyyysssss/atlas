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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.ws.rs.NotSupportedException;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                    .where("objectClass").is("user")
                    .and("sAMAccountName").is(username);
            List<DirContextAdapter> users = ldapTemplateFactory.getQueryTemplate(provider, settings)
                    .search(query, (ContextMapper<DirContextAdapter>) ctx -> (DirContextAdapter) ctx);
            log.info("LDAP search result count: {}", users.size());

            for (DirContextAdapter user : users) {
                log.info("LDAP USER DN: {}", user.getDn());

                Attributes attributes = user.getAttributes();

                NamingEnumeration<? extends Attribute> enumeration = attributes.getAll();

                while (enumeration.hasMore()) {
                    Attribute attribute = enumeration.next();
                    NamingEnumeration<?> values = attribute.getAll();
                    while (values.hasMore()) {
                        Object value = values.next();
                        if (value instanceof byte[] bytes) {
                            if ("objectGUID".equalsIgnoreCase(attribute.getID())) {
                                value = objectGuidToUuid(bytes);
                            } else if ("objectSid".equalsIgnoreCase(attribute.getID())) {
                                value = objectSidToString(bytes);
                            } else {
                                value = HexFormat.of().formatHex(bytes);
                            }
                            log.info("LDAP attribute: {} = {}", attribute.getID(), value);
                        } else {
                            log.info("LDAP attribute: {} = {}", attribute.getID(), value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("LDAP authenticate error: ", e);
            throw new BusinessException("LDAP authentication failed");
        }
        return null;
    }


    private UUID objectGuidToUuid(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        long mostSigBits = ((long) Integer.reverseBytes(buffer.getInt()) << 32)
                | ((long) Short.toUnsignedInt(Short.reverseBytes(buffer.getShort())) << 16)
                | Short.toUnsignedInt(Short.reverseBytes(buffer.getShort()));

        long leastSigBits = buffer.getLong();

        return new UUID(mostSigBits, leastSigBits);
    }

    private String objectSidToString(byte[] sid) {
        if (sid == null || sid.length < 8) {
            throw new IllegalArgumentException("Invalid SID");
        }

        int revision = sid[0] & 0xFF;
        int subAuthorityCount = sid[1] & 0xFF;

        // IdentifierAuthority 是 6 字节，大端
        long identifierAuthority = 0;
        for (int i = 2; i < 8; i++) {
            identifierAuthority = (identifierAuthority << 8) | (sid[i] & 0xFFL);
        }

        StringBuilder result = new StringBuilder();
        result.append("S-")
                .append(revision)
                .append("-")
                .append(identifierAuthority);

        // SubAuthority 是 4 字节，小端
        for (int i = 0; i < subAuthorityCount; i++) {
            int offset = 8 + i * 4;

            long subAuthority = (sid[offset] & 0xFFL)
                            | ((sid[offset + 1] & 0xFFL) << 8)
                            | ((sid[offset + 2] & 0xFFL) << 16)
                            | ((sid[offset + 3] & 0xFFL) << 24);

            result.append("-").append(subAuthority);
        }

        return result.toString();
    }

}
