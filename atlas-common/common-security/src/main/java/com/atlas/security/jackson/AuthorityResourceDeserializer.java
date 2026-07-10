package com.atlas.security.jackson;

import com.atlas.common.core.api.user.dto.AuthorityResource;
import com.atlas.security.utils.JsonNodeUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 10:14
 */
public class AuthorityResourceDeserializer extends JsonDeserializer<AuthorityResource> {
    @Override
    public AuthorityResource deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode root = mapper.readTree(jsonParser);
        AuthorityResource authorityUrl = new AuthorityResource();
        List<String> method = JsonNodeUtils.findValue(root, "method", new TypeReference<>() {
            @Override
            public Type getType() {
                return super.getType();
            }
        }, mapper);
        String url = JsonNodeUtils.findStringValue(root, "url");
        authorityUrl.setMethod(method);
        authorityUrl.setUrl(url);
        return authorityUrl;
    }
}
