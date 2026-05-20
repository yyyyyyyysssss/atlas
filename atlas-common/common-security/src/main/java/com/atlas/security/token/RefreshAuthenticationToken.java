package com.atlas.security.token;

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
 * @Date 2026/2/24 15:28
 */
public class RefreshAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private Object credentials;
    private String oldTokenId;

    public RefreshAuthenticationToken(Object principal, Object credentials) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        this.setAuthenticated(false);
    }

    public RefreshAuthenticationToken(Object principal, Object credentials,String oldTokenId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.oldTokenId = oldTokenId;
        super.setAuthenticated(true);
    }


    public static RefreshAuthenticationToken unauthenticated(Object principal, Object credentials) {
        return new RefreshAuthenticationToken(principal, credentials);
    }

    public static RefreshAuthenticationToken authenticated(Object principal, Object credentials,String oldTokenId, Collection<? extends GrantedAuthority> authorities) {
        return new RefreshAuthenticationToken(principal, credentials,oldTokenId, authorities);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public String getOldTokenId() {
        return oldTokenId;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = RefreshAuthenticationToken.RefreshAuthenticationTokenDeserializer.class)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class RefreshAuthenticationTokenMixin {
    }

    static class RefreshAuthenticationTokenDeserializer extends JsonDeserializer<RefreshAuthenticationToken> {

        @Override
        public RefreshAuthenticationToken deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            ObjectMapper mapper = (ObjectMapper)jsonParser.getCodec();
            JsonNode jsonNode = mapper.readTree(jsonParser);

            Object principal = JsonNodeUtils.getPrincipal(jsonNode, mapper);

            Object credentials = JsonNodeUtils.getCredentials(jsonNode);

            boolean authenticated = JsonNodeUtils.getAuthenticated(jsonNode);

            String oldTokenId = JsonNodeUtils.findStringValue(jsonNode, "oldTokenId");

            List<GrantedAuthority> authorities =JsonNodeUtils.getAuthorities(jsonNode, mapper);

            RefreshAuthenticationToken token = !authenticated ? RefreshAuthenticationToken.unauthenticated(principal, credentials) : RefreshAuthenticationToken.authenticated(principal, credentials,oldTokenId, authorities);

            Object details = JsonNodeUtils.getDetails(jsonNode,mapper);

            token.setDetails(details);
            return token;
        }

    }

}
