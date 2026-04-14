package com.atlas.security.jackson;

import com.atlas.security.model.SecurityUser;
import com.atlas.security.utils.JsonNodeUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SecurityUserDeserializer extends JsonDeserializer<SecurityUser> {

    private final TypeReference<List<GrantedAuthority>> GRANTED_AUTHORITY_LIST = new TypeReference<>() {
    };

    @Override
    public SecurityUser deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode root = mapper.readTree(jsonParser);
        SecurityUser securityUser = new SecurityUser();
        String id = JsonNodeUtils.findNumberValue(root, "id");
        String username = JsonNodeUtils.findStringValue(root, "username");
        String password = JsonNodeUtils.findStringValue(root, "password");
        String fullName = JsonNodeUtils.findStringValue(root, "fullName");
        String tokenId = JsonNodeUtils.findStringValue(root, "tokenId");
        Set<Integer> dataScopes = (Set<Integer>) mapper.readValue(this.readJsonNode(root, "dataScopes").traverse(mapper), new TypeReference<>() {});
        String orgId = JsonNodeUtils.findNumberValue(root, "orgId");
        List<? extends GrantedAuthority> authorities = (List)mapper.readValue(this.readJsonNode(root, "authorities").traverse(mapper), GRANTED_AUTHORITY_LIST);
        securityUser.setId(Long.parseLong(id));
        securityUser.setUsername(username);
        securityUser.setPassword(password);
        securityUser.setFullName(fullName);
        securityUser.setTokenId(tokenId);

        securityUser.setDataScopes(dataScopes);
        securityUser.setOrgId(orgId != null ? Long.parseLong(orgId) : null);
        securityUser.setAuthorities(authorities);
        return securityUser;
    }

    private JsonNode readJsonNode(JsonNode jsonNode, String field) {
        return (JsonNode)(jsonNode.has(field) ? jsonNode.get(field) : MissingNode.getInstance());
    }

}
