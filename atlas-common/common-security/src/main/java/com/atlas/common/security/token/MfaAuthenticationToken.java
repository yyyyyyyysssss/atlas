package com.atlas.common.security.token;

import com.atlas.common.security.enums.AuthAssuranceLevel;
import com.atlas.common.security.model.MfaType;
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
 * @Description 用于三方登录的身份认证token
 * @Author ys
 * @Date 2024/8/6 9:29
 */
@Getter
public class MfaAuthenticationToken extends AbstractAuthenticationToken implements AssuranceLevelAware {

    private final Object principal;
    private Object credentials;
    private MfaType mfaType;

    private AuthAssuranceLevel authAssuranceLevel;

    public MfaAuthenticationToken(Object principal, Object credentials, MfaType mfaType) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        this.mfaType = mfaType;
        this.setAuthenticated(false);
    }

    public MfaAuthenticationToken(Object principal, MfaType mfaType, AuthAssuranceLevel authAssuranceLevel, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        this.mfaType = mfaType;
        this.authAssuranceLevel = authAssuranceLevel;
        super.setAuthenticated(true);
    }

    public static MfaAuthenticationToken unauthenticated(Object principal, Object credentials, MfaType mfaType) {
        return new MfaAuthenticationToken(principal, credentials, mfaType);
    }

    public static MfaAuthenticationToken authenticated(Object principal, MfaType mfaType, AuthAssuranceLevel authAssuranceLevel, Collection<? extends GrantedAuthority> authorities) {
        return new MfaAuthenticationToken(principal, mfaType, authAssuranceLevel, authorities);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public AuthAssuranceLevel getAssuranceLevel() {

        return authAssuranceLevel;
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

    static class MfaAuthenticationTokenDeserializer extends JsonDeserializer<MfaAuthenticationToken> {

        @Override
        public MfaAuthenticationToken deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            JsonNode jsonNode = mapper.readTree(jsonParser);

            Object principal = JsonNodeUtils.getPrincipal(jsonNode, mapper);

            Object credentials = JsonNodeUtils.getCredentials(jsonNode);

            boolean authenticated = JsonNodeUtils.getAuthenticated(jsonNode);

            String mfaTypeStr = JsonNodeUtils.findStringValue(jsonNode, "mfaType");

            MfaType mfaType = mfaTypeStr == null ? null : MfaType.valueOf(mfaTypeStr);

            String assuranceLevelStr = JsonNodeUtils.findStringValue(jsonNode, "authAssuranceLevel");

            AuthAssuranceLevel assuranceLevel = assuranceLevelStr == null ? null : AuthAssuranceLevel.valueOf(assuranceLevelStr);

            List<GrantedAuthority> authorities = JsonNodeUtils.getAuthorities(jsonNode, mapper);

            MfaAuthenticationToken token = !authenticated
                    ?
                    MfaAuthenticationToken.unauthenticated(principal, credentials, mfaType)
                    :
                    MfaAuthenticationToken.authenticated(principal, mfaType, assuranceLevel, authorities);

            Object details = JsonNodeUtils.getDetails(jsonNode, mapper);

            token.setDetails(details);

            return token;
        }

    }

}
