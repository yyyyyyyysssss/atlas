package com.atlas.security.jackson;


import com.atlas.security.model.AuthorityUrl;
import com.atlas.security.model.RequestUrlAuthority;
import com.atlas.security.utils.JsonNodeUtils;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 10:21
 */
public class RequestUrlAuthorityDeserializer extends JsonDeserializer<RequestUrlAuthority> {
    @Override
    public RequestUrlAuthority deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode root = mapper.readTree(jsonParser);
        RequestUrlAuthority requestAuthority = new RequestUrlAuthority();
        String code = JsonNodeUtils.findStringValue(root, "code");
        List<AuthorityUrl> urls = JsonNodeUtils.findValue(root, "urls", new TypeReference<List<AuthorityUrl>>() {}, mapper);
        requestAuthority.setCode(code);
        requestAuthority.setUrls(urls);
        return requestAuthority;
    }
}
