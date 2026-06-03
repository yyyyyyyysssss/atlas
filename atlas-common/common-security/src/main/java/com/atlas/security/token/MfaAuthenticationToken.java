package com.atlas.security.token;

import com.atlas.security.enums.AuthAssuranceLevel;
import com.atlas.security.model.MfaType;
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
 * @Description 用于三方登录的身份认证token
 * @Author ys
 * @Date 2024/8/6 9:29
 */
public class MfaAuthenticationToken extends AbstractAuthenticationToken implements AssuranceLevelAware {

    private final Object principal;
    private Object credentials;
    private MfaType mfaType;

    public MfaAuthenticationToken(Object principal, Object credentials, MfaType mfaType) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        this.mfaType = mfaType;
        this.setAuthenticated(false);
    }

    public MfaAuthenticationToken(Object principal, Object credentials,MfaType mfaType, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.mfaType = mfaType;
        super.setAuthenticated(true);
    }

    public static MfaAuthenticationToken unauthenticated(Object principal, Object credentials, MfaType mfaType) {
        return new MfaAuthenticationToken(principal, credentials,mfaType);
    }

    public static MfaAuthenticationToken authenticated(Object principal, Object credentials,MfaType mfaType, Collection<? extends GrantedAuthority> authorities) {
        return new MfaAuthenticationToken(principal, credentials,mfaType, authorities);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public MfaType getMfaType() {
        return mfaType;
    }

    @Override
    public AuthAssuranceLevel getAssuranceLevel() {
        return AuthAssuranceLevel.MEDIUM;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = MfaAuthenticationTokenDeserializer.class)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class MfaAuthenticationTokenMixin {
    }

    static class MfaAuthenticationTokenDeserializer extends JsonDeserializer<MfaAuthenticationToken>{

        private static final TypeReference<List<GrantedAuthority>> GRANTED_AUTHORITY_LIST = new TypeReference<>() {
        };
        private static final TypeReference<Object> OBJECT = new TypeReference<>() {
        };

        @Override
        public MfaAuthenticationToken deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            ObjectMapper mapper = (ObjectMapper)jsonParser.getCodec();
            JsonNode jsonNode = mapper.readTree(jsonParser);

            Object principal = JsonNodeUtils.getPrincipal(jsonNode, mapper);

            Object credentials = JsonNodeUtils.getCredentials(jsonNode);

            boolean authenticated = JsonNodeUtils.getAuthenticated(jsonNode);

            String mfaTypeStr = JsonNodeUtils.findStringValue(jsonNode, "mfaType");

            MfaType mfaType = MfaType.valueOf(mfaTypeStr);

            List<GrantedAuthority> authorities =JsonNodeUtils.getAuthorities(jsonNode, mapper);

            MfaAuthenticationToken token = !authenticated ? MfaAuthenticationToken.unauthenticated(principal, credentials,mfaType) : MfaAuthenticationToken.authenticated(principal, credentials,mfaType, authorities);

            Object details = JsonNodeUtils.getDetails(jsonNode,mapper);

            token.setDetails(details);

            return token;
        }

    }

}
