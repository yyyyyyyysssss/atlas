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
 * @Date 2026/5/19 17:27
 */
public class CaptchaAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private Object credentials;
    private final String captchaType;

    public CaptchaAuthenticationToken(Object principal, Object credentials, String captchaType) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        this.captchaType = captchaType;
        this.setAuthenticated(false);
    }

    public CaptchaAuthenticationToken(Object principal, Object credentials, String captchaType, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.captchaType = captchaType;
        super.setAuthenticated(true);
    }


    public static CaptchaAuthenticationToken unauthenticated(Object principal, Object credentials, String captchaType) {
        return new CaptchaAuthenticationToken(principal, credentials, captchaType);
    }

    public static CaptchaAuthenticationToken authenticated(Object principal, Object credentials, String captchaType, Collection<? extends GrantedAuthority> authorities) {
        return new CaptchaAuthenticationToken(principal, credentials, captchaType, authorities);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public String getCaptchaType() {
        return captchaType;
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = CaptchaAuthenticationToken.CaptchaAuthenticationTokenDeserializer.class)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class CaptchaAuthenticationTokenMixin {
    }

    static class CaptchaAuthenticationTokenDeserializer extends JsonDeserializer<CaptchaAuthenticationToken> {

        @Override
        public CaptchaAuthenticationToken deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            ObjectMapper mapper = (ObjectMapper)jsonParser.getCodec();
            JsonNode jsonNode = mapper.readTree(jsonParser);

            Object principal = JsonNodeUtils.getPrincipal(jsonNode, mapper);

            Object credentials = JsonNodeUtils.getCredentials(jsonNode);

            boolean authenticated = JsonNodeUtils.getAuthenticated(jsonNode);

            String captchaType = JsonNodeUtils.findStringValue(jsonNode, "captchaType");

            List<GrantedAuthority> authorities =JsonNodeUtils.getAuthorities(jsonNode, mapper);

            CaptchaAuthenticationToken token = !authenticated ? CaptchaAuthenticationToken.unauthenticated(principal, credentials,captchaType) : CaptchaAuthenticationToken.authenticated(principal, credentials,captchaType, authorities);

            Object details = JsonNodeUtils.getDetails(jsonNode,mapper);

            token.setDetails(details);

            return token;
        }

    }
}
