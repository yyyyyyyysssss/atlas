package com.atlas.common.security.token;

import com.atlas.common.security.enums.AuthAssuranceLevel;
import com.atlas.common.security.utils.JsonNodeUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/19 10:44
 */
@Getter
public class CertificateAuthenticationToken extends AbstractAuthenticationToken implements AssuranceLevelAware {

    private Object principal;

    private String clientCertificate;

    private String clientCertificateChain;

    public CertificateAuthenticationToken(String clientCertificate, String clientCertificateChain) {
        super(null);
        this.principal = null;
        this.clientCertificate = clientCertificate;
        this.clientCertificateChain = clientCertificateChain;
        super.setAuthenticated(false);
    }

    public CertificateAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        super.setAuthenticated(true);
    }

    public static CertificateAuthenticationToken unauthenticated(String clientCertificate, String clientCertificateChain) {
        return new CertificateAuthenticationToken(clientCertificate, clientCertificateChain);
    }

    public static CertificateAuthenticationToken authenticated(Object principal, Collection<? extends GrantedAuthority> authorities) {
        return new CertificateAuthenticationToken(principal, authorities);
    }

    @Override
    public AuthAssuranceLevel getAssuranceLevel() {
        return AuthAssuranceLevel.HIGH;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = CertificateAuthenticationToken.CertificateAuthenticationTokenDeserializer.class)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class CertificateAuthenticationTokenMixin {
    }

    static class CertificateAuthenticationTokenDeserializer extends JsonDeserializer<CertificateAuthenticationToken> {

        @Override
        public CertificateAuthenticationToken deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            ObjectMapper mapper = (ObjectMapper)jsonParser.getCodec();
            JsonNode jsonNode = mapper.readTree(jsonParser);

            Object principal = JsonNodeUtils.getPrincipal(jsonNode, mapper);

            boolean authenticated = JsonNodeUtils.getAuthenticated(jsonNode);

            String clientCertificate = JsonNodeUtils.findStringValue(jsonNode, "clientCertificate");

            String clientCertificateChain = JsonNodeUtils.findStringValue(jsonNode, "clientCertificateChain");

            List<GrantedAuthority> authorities =JsonNodeUtils.getAuthorities(jsonNode, mapper);

            CertificateAuthenticationToken token = !authenticated ?
                    CertificateAuthenticationToken.unauthenticated(clientCertificate,clientCertificateChain)
                    :
                    CertificateAuthenticationToken.authenticated(principal, authorities);

            Object details = JsonNodeUtils.getDetails(jsonNode,mapper);

            token.setDetails(details);

            return token;
        }

    }

}
