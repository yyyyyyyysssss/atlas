package com.atlas.security.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Description
 * @Author ys
 * @Date 2024/9/4 12:48
 */
public class JsonNodeUtils {

    private static final TypeReference<List<GrantedAuthority>> GRANTED_AUTHORITY_LIST = new TypeReference<List<GrantedAuthority>>() {};

    public static JsonNode getChildNode(JsonNode jsonNode, String fieldName) {
        if (jsonNode == null) {
            return MissingNode.getInstance();
        }
        return jsonNode.has(fieldName) ? jsonNode.get(fieldName) : MissingNode.getInstance();
    }

    public static Object getPrincipal(JsonNode jsonNode, ObjectMapper mapper) throws IOException {
        JsonNode principalNode = getChildNode(jsonNode, "principal");
        return principalNode.isObject() ? mapper.readValue(principalNode.traverse(mapper), Object.class) : principalNode.asText();
    }

    public static Object getCredentials(JsonNode jsonNode) {
        JsonNode credentialsNode = getChildNode(jsonNode, "credentials");
        return !credentialsNode.isNull() && !credentialsNode.isMissingNode() ? credentialsNode.asText() : null;
    }

    public static boolean getAuthenticated(JsonNode jsonNode) {
        JsonNode authNode = getChildNode(jsonNode, "authenticated");
        return !authNode.isMissingNode() && authNode.asBoolean();
    }

    public static List<GrantedAuthority> getAuthorities(JsonNode jsonNode, ObjectMapper mapper) {
        JsonNode authoritiesNode = getChildNode(jsonNode, "authorities");
        if (authoritiesNode.isMissingNode() || authoritiesNode.isNull() || !authoritiesNode.isContainerNode()) {
            return Collections.emptyList(); // 兜底返回空列表，防止上游抛 NPE
        }
        return mapper.convertValue(authoritiesNode, GRANTED_AUTHORITY_LIST);
    }

    public static Object getDetails(JsonNode jsonNode, ObjectMapper mapper) {
        JsonNode detailsNode = getChildNode(jsonNode, "details");
        if (detailsNode.isMissingNode() || detailsNode.isNull()) {
            return null;
        }
        return detailsNode.isContainerNode()
                ? mapper.convertValue(detailsNode, Object.class)
                : detailsNode.asText();
    }

    public static String findStringValue(JsonNode jsonNode, String fieldName) {
        if (jsonNode == null) {
            return null;
        }
        JsonNode value = jsonNode.findValue(fieldName);
        return (value != null && value.isTextual()) ? value.asText() : null;
    }

    public static String findNumberValue(JsonNode jsonNode, String fieldName) {
        if (jsonNode == null) {
            return null;
        }
        JsonNode value = jsonNode.findValue(fieldName);
        return (value != null && value.isNumber()) ? value.asText() : null;
    }

    public static <T> T findValue(JsonNode jsonNode, String fieldName, TypeReference<T> valueTypeReference,
                           ObjectMapper mapper) {
        if (jsonNode == null) {
            return null;
        }
        JsonNode value = jsonNode.findValue(fieldName);
        return (value != null && value.isContainerNode()) ? mapper.convertValue(value, valueTypeReference) : null;
    }

    public static JsonNode findObjectNode(JsonNode jsonNode, String fieldName) {
        if (jsonNode == null) {
            return null;
        }
        JsonNode value = jsonNode.findValue(fieldName);
        return (value != null && value.isObject()) ? value : null;
    }

}
