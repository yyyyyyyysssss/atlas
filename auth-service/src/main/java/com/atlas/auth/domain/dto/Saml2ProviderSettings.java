package com.atlas.auth.domain.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public record Saml2ProviderSettings(
        String entityId,
        String clientName,
        Acs acs,
        AssertingParty assertingparty
) implements SsoSettings {

    public record Acs(
            String location,
            Saml2MessageBinding binding
    ) {}

    public record AssertingParty(
            String entityId,
            SingleSignOn singlesignon,
            Verification verification,
            Mapping mappings
    ) {}

    public record SingleSignOn(
            String url,
            Saml2MessageBinding binding,
            boolean signRequest
    ) {}

    public record Verification(
            List<Credential> credentials
    ) {}

    public record Credential(
            @JsonDeserialize(using = Credential.ResourceDeserializer.class)
            Resource certificateLocation
    ) {

        // 将反序列化器定义为静态内部类
        public static class ResourceDeserializer extends JsonDeserializer<Resource> {
            private static final ResourceLoader loader = new DefaultResourceLoader();
            @Override
            public Resource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return loader.getResource(p.getValueAsString());
            }
        }

    }

    public record Mapping(
            String sub,
            String email,
            String emailVerified,
            String fullName,
            String avatar
    ) {}
}