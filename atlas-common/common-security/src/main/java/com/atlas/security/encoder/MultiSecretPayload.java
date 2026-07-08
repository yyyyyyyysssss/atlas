package com.atlas.security.encoder;

import com.atlas.common.core.utils.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public record MultiSecretPayload(
        String type,
        List<SecretItem> secrets
) {

    public static final String PREFIX = "{multi}";
    public static final String TYPE = "multi";

    public MultiSecretPayload {
        if (!TYPE.equals(type)) {
            throw new IllegalArgumentException(
                    "Unsupported type: " + type
            );
        }
        if (secrets == null || secrets.isEmpty()) {
            throw new IllegalArgumentException(
                    "Secrets cannot be empty"
            );
        }
        secrets = List.copyOf(secrets);
    }


    public static String encode(String... secrets) {
        if (secrets == null || secrets.length == 0) {
            throw new IllegalArgumentException("Secrets cannot be empty");
        }

        MultiSecretPayload payload  = new MultiSecretPayload(
                TYPE,
                Arrays.stream(secrets)
                        .map(SecretItem::of)
                        .toList()
        );
        String json = JsonUtils.toJson(payload);
        return PREFIX + Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public static MultiSecretPayload decode(String secret) {
        if (secret == null || !secret.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Invalid multi secret format");
        }
        String base64 = secret.substring(PREFIX.length());
        String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        MultiSecretPayload payload = JsonUtils.parseObject(json, MultiSecretPayload.class);
        if (!TYPE.equals(payload.type())) {
            throw new IllegalArgumentException("Unsupported multi secret type: " + payload.type());
        }
        return payload;
    }



    public record SecretItem(
            String id,
            String value
    ) {

        public static SecretItem of(String value) {
            return new SecretItem(null, value);
        }

        public static SecretItem of(String id, String value) {
            return new SecretItem(id, value);
        }

    }

}
