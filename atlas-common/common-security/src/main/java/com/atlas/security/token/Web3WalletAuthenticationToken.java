package com.atlas.security.token;

import com.atlas.security.enums.AuthAssuranceLevel;
import com.atlas.security.utils.JsonNodeUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/11 11:41
 */
public class Web3WalletAuthenticationToken extends AbstractAuthenticationToken implements AssuranceLevelAware{

    private final Object principal;

    private Object credentials;

    public Web3WalletAuthenticationToken(Object principal, Object credentials) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        this.setAuthenticated(false);
    }

    public Web3WalletAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);
    }

    public static Web3WalletAuthenticationToken unauthenticated(Object principal, Object credentials) {
        return new Web3WalletAuthenticationToken(principal, credentials);
    }

    public static Web3WalletAuthenticationToken authenticated(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        return new Web3WalletAuthenticationToken(principal, credentials, authorities);
    }

    public Object getCredentials() {
        return this.credentials;
    }

    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }

    @Override
    public AuthAssuranceLevel getAssuranceLevel() {
        return AuthAssuranceLevel.HIGH;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = Web3WalletAuthenticationTokenDeserializer.class)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Web3WalletAuthenticationTokenMixin {
    }

    static class Web3WalletAuthenticationTokenDeserializer extends JsonDeserializer<Web3WalletAuthenticationToken> {

        @Override
        public Web3WalletAuthenticationToken deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            ObjectMapper mapper = (ObjectMapper)jsonParser.getCodec();
            JsonNode jsonNode = mapper.readTree(jsonParser);

            Object principal = JsonNodeUtils.getPrincipal(jsonNode, mapper);

            Object credentials = JsonNodeUtils.getCredentials(jsonNode);

            boolean authenticated = JsonNodeUtils.getAuthenticated(jsonNode);

            List<GrantedAuthority> authorities =JsonNodeUtils.getAuthorities(jsonNode, mapper);

            Web3WalletAuthenticationToken token = !authenticated ? Web3WalletAuthenticationToken.unauthenticated(principal, credentials) : Web3WalletAuthenticationToken.authenticated(principal, credentials, authorities);

            Object details = JsonNodeUtils.getDetails(jsonNode,mapper);

            token.setDetails(details);

            return token;
        }

    }

}
