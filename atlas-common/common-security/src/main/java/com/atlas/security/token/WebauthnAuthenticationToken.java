package com.atlas.security.token;

import com.atlas.security.utils.JsonNodeUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
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
 * @Date 2026/5/26 14:08
 */
public class WebauthnAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    private Object credentials;

    private final WebauthnAuthenticationRequest webAuthnRequest;

    public WebauthnAuthenticationToken(WebauthnAuthenticationRequest webAuthnRequest) {
        super(null);
        this.principal = null;
        this.credentials = null;
        this.webAuthnRequest = webAuthnRequest;
        this.setAuthenticated(false);
    }

    public WebauthnAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.webAuthnRequest = null;
        super.setAuthenticated(true);
    }

    public Object getCredentials() {
        return this.credentials;
    }

    public Object getPrincipal() {
        return this.principal;
    }

    public WebauthnAuthenticationRequest getWebAuthnRequest() {
        return this.webAuthnRequest;
    }

    public static WebauthnAuthenticationToken unauthenticated(WebauthnAuthenticationRequest webAuthnRequest) {
        return new WebauthnAuthenticationToken(webAuthnRequest);
    }

    public static WebauthnAuthenticationToken authenticated(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        return new WebauthnAuthenticationToken(principal, credentials, authorities);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = WebAuthnAuthenticationTokenDeserializer.class)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class WebAuthnAuthenticationTokenMixin {
    }

    static class WebAuthnAuthenticationTokenDeserializer extends JsonDeserializer<WebauthnAuthenticationToken> {

        @Override
        public WebauthnAuthenticationToken deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            ObjectMapper mapper = (ObjectMapper)jsonParser.getCodec();
            JsonNode jsonNode = mapper.readTree(jsonParser);

            Object principal = JsonNodeUtils.getPrincipal(jsonNode, mapper);

            Object credentials = JsonNodeUtils.getCredentials(jsonNode);

            boolean authenticated = JsonNodeUtils.getAuthenticated(jsonNode);

            WebauthnAuthenticationRequest webAuthnRequest = JsonNodeUtils.findValue(jsonNode, "webAuthnRequest", new TypeReference<WebauthnAuthenticationRequest>() {}, mapper);

            List<GrantedAuthority> authorities =JsonNodeUtils.getAuthorities(jsonNode, mapper);

            WebauthnAuthenticationToken token = !authenticated ? WebauthnAuthenticationToken.unauthenticated(webAuthnRequest) : WebauthnAuthenticationToken.authenticated(principal, credentials, authorities);

            Object details = JsonNodeUtils.getDetails(jsonNode,mapper);

            token.setDetails(details);

            return token;
        }

    }
}
