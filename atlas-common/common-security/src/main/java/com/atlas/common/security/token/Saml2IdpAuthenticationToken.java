package com.atlas.common.security.token;

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
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/10 17:51
 */
public class Saml2IdpAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    private Object credentials;

    private final String samlRequest;

    private final String binding;

    private final String relayState;

    // 原始 QueryString Redirect Binding验签需要
    private final String rawQueryString;

    // 未认证 Token Converter 创建
    public Saml2IdpAuthenticationToken(String samlRequest, String binding, String relayState, String rawQueryString) {
        super(null);
        this.principal = null;
        this.credentials = samlRequest;
        this.samlRequest = samlRequest;
        this.binding = binding;
        this.relayState = relayState;
        this.rawQueryString = rawQueryString;
    }

    // 已认证 Token Provider 创建
    public Saml2IdpAuthenticationToken(Object principal, String binding, String relayState, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.binding = binding;
        this.relayState = relayState;
        this.credentials = null;
        this.samlRequest = null;
        this.rawQueryString = null;
        super.setAuthenticated(true);
    }


    public static Saml2IdpAuthenticationToken unauthenticated(String samlRequest, String binding, String relayState, String rawQueryString) {
        return new Saml2IdpAuthenticationToken(samlRequest, binding, relayState, rawQueryString);
    }

    public static Saml2IdpAuthenticationToken authenticated(Object principal, String binding, String relayState, Collection<? extends GrantedAuthority> authorities) {
        return new Saml2IdpAuthenticationToken(principal, binding, relayState, authorities);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public String getSamlRequest() {
        return samlRequest;
    }

    public String getBinding() {
        return binding;
    }

    public String getRelayState() {
        return relayState;
    }

    public String getRawQueryString() {
        return rawQueryString;
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = Saml2IdpAuthenticationToken.Saml2IdpAuthenticationTokenDeserializer.class)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Saml2IdpAuthenticationTokenMixin {
    }

    static class Saml2IdpAuthenticationTokenDeserializer extends JsonDeserializer<Saml2IdpAuthenticationToken> {

        @Override
        public Saml2IdpAuthenticationToken deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            JsonNode jsonNode = mapper.readTree(jsonParser);

            Object principal = JsonNodeUtils.getPrincipal(jsonNode, mapper);

            boolean authenticated = JsonNodeUtils.getAuthenticated(jsonNode);

            String samlRequest = JsonNodeUtils.findStringValue(jsonNode, "samlRequest");
            String binding = JsonNodeUtils.findStringValue(jsonNode, "binding");
            String relayState = JsonNodeUtils.findStringValue(jsonNode, "relayState");
            String rawQueryString = JsonNodeUtils.findStringValue(jsonNode, "rawQueryString");

            List<GrantedAuthority> authorities = JsonNodeUtils.getAuthorities(jsonNode, mapper);

            Saml2IdpAuthenticationToken token = !authenticated
                    ? Saml2IdpAuthenticationToken.unauthenticated(samlRequest, binding, relayState, rawQueryString)
                    :
                    Saml2IdpAuthenticationToken.authenticated(principal,binding, relayState, authorities);

            Object details = JsonNodeUtils.getDetails(jsonNode, mapper);

            token.setDetails(details);
            return token;
        }

    }

}
