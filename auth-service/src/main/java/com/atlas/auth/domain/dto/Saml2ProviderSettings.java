package com.atlas.auth.domain.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

public record Saml2ProviderSettings(
        String entityId,
        String clientName,
        Acs acs,
        AssertingParty assertingparty
) implements SsoSettings {

    public record Acs(
            String location,
            Saml2MessageBinding binding
    ) {
    }

    public record AssertingParty(
            String entityId,
            SingleSignOn singlesignon,
            Verification verification,
            Mapping mappings
    ) {
    }

    public record SingleSignOn(
            String url,
            Saml2MessageBinding binding,
            boolean signRequest
    ) {
    }

    public record Verification(
            List<Credential> credentials
    ) {
    }

    public record Credential(
            @JsonDeserialize(using = Credential.ResourceDeserializer.class)
            Resource certificateLocation
    ) {

        // 将反序列化器定义为静态内部类
        public static class ResourceDeserializer extends JsonDeserializer<Resource> {
            private static final ResourceLoader loader = new DefaultResourceLoader();
            private static final String TEXT_PREFIX = "text:";
            private static final String CLASSPATH_PREFIX = "classpath:";
            private static final String FILE_PREFIX = "file:";
            private static final String ENV_PREFIX = "env:";

            @Override
            public Resource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                if (value == null) {
                    return null;
                }
                // 处理 env: 模式 (环境变量或系统属性)
                if (value.startsWith(ENV_PREFIX)) {
                    String key = value.substring(ENV_PREFIX.length()).trim();
                    // 优先从环境变量获取，没有则从 Java -D 属性获取
                    String envValue = System.getenv(key);
                    if (envValue == null) {
                        envValue = System.getProperty(key);
                    }
                    if (envValue == null || envValue.trim().isEmpty()) {
                        throw MismatchedInputException.from(
                                p, Resource.class, "Environment variable or System property '" + key + "' is not set."
                        );
                    }
                    // 拿到环境变量的值后继续调用此方法 这样环境变量里既可以配普通路径，也可以直接配 Base64 文本等
                    return parseActualResource(envValue, p);
                }
                return parseActualResource(value, p);
            }

            private Resource parseActualResource(String value, JsonParser p) throws MismatchedInputException {
                // 判断是否为纯文本模式
                if (value.startsWith(TEXT_PREFIX)) {
                    // 截取 text: 后面的纯 Base64 字符串
                    String base64Str = value.substring(TEXT_PREFIX.length());
                    // 清理字符串中的空格和换行符，防止干扰解码
                    String cleanBase64 = base64Str.replaceAll("\\s", "");
                    // 将其解码为标准的 DER 二进制字节数组
                    byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
                    return new ByteArrayResource(decodedBytes, "Database-configured Base64 Certificate");
                }

                // 处理类路径模式
                if (value.startsWith(CLASSPATH_PREFIX)) {
                    Resource resource = loader.getResource(value);
                    if (!resource.exists()) {
                        throw MismatchedInputException.from(
                                p, Resource.class, "Certificate file not found in classpath: " + value
                        );
                    }
                    return resource;
                }

                // 处理本地文件路径模式
                if (value.startsWith(FILE_PREFIX)) {
                    Resource resource = loader.getResource(value);
                    if (!resource.exists()) {
                        throw MismatchedInputException.from(
                                p, Resource.class, "Local certificate file not found at: " + value
                        );
                    }
                    return resource;
                }

                throw MismatchedInputException.from(
                        p,
                        Resource.class,
                        "Unsupported certificate format! Only 'text:', 'classpath:', 'file:', or 'env:' prefixes are allowed. Current value: " + value
                );
            }

        }

    }

    public record Mapping(
            String sub,
            String email,
            String emailVerified,
            String fullName,
            String avatar
    ) {
    }
}