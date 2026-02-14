package com.atlas.security.jackson;

import com.atlas.security.model.AuthorityUrl;
import com.atlas.security.utils.JsonNodeUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 10:14
 */
public class AuthorityUrlDeserializer extends JsonDeserializer<AuthorityUrl> {
    @Override
    public AuthorityUrl deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode root = mapper.readTree(jsonParser);
        AuthorityUrl authorityUrl = new AuthorityUrl();
        String method = JsonNodeUtils.findStringValue(root, "method");
        String url = JsonNodeUtils.findStringValue(root, "url");
        authorityUrl.setMethod(method);
        authorityUrl.setUrl(url);
        return authorityUrl;
    }
}
