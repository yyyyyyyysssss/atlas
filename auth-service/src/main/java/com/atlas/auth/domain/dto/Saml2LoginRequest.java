package com.atlas.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        property = "@class"
)
public record Saml2LoginRequest(
        String registrationId,
        String name,
        Map<String, List<String>> attributes,
        List<String> sessionIndexes
) implements ThirdPartyLoginRequest {

    public static Saml2LoginRequest from(Saml2AuthenticatedPrincipal principal) {

        Map<String, List<String>> attributes =
                principal.getAttributes().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .map(String::valueOf)
                                        .toList()
                        ));

        return new Saml2LoginRequest(
                principal.getRelyingPartyRegistrationId(),
                principal.getName(),
                attributes,
                principal.getSessionIndexes()
        );
    }

    public String getFirstAttribute(String attributeName) {
        if (!StringUtils.hasText(attributeName)) {
            return null;
        }

        List<String> values = attributes.get(attributeName);

        return (values == null || values.isEmpty())
                ? null
                : values.getFirst();
    }

}
