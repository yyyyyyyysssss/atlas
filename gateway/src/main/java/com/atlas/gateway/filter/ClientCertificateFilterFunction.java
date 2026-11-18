package com.atlas.gateway.filter;

import com.atlas.gateway.enums.CertificateMode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/18 16:36
 */
public final class ClientCertificateFilterFunction {

    private static final String CERTIFICATE_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    private ClientCertificateFilterFunction() {
    }

    public static HandlerFilterFunction<ServerResponse, ServerResponse> forwardClientCertificate(String headerName, boolean required, CertificateMode mode) {
        return HandlerFilterFunction.ofRequestProcessor(request -> {

            // 根据配置的模式获取客户端证书
            X509Certificate certificate = switch (mode) {
                case LOCAL -> resolveLocalCertificate(request);
                case FORWARDED -> resolveForwardedCertificate(request, headerName);
            };
            if (certificate == null && required) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Client certificate is required");
            }
            try {
                String encoded;
                if (certificate != null) {
                    // 统一转发 Base64(DER)
                    encoded = Base64.getEncoder().encodeToString(certificate.getEncoded());
                } else {
                    encoded = null;
                }
                return ServerRequest.from(request)
                        .headers(h -> {
                            h.remove(headerName);
                            if(encoded != null){
                                h.add(headerName, encoded);
                            }
                        })
                        .build();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to encode client certificate", e);
            }
        });
    }

    // 从本地 HTTPS Servlet 请求属性中获取证书
    private static X509Certificate resolveLocalCertificate(ServerRequest request) {
        X509Certificate[] certificates = (X509Certificate[]) request
                .servletRequest()
                .getAttribute(CERTIFICATE_ATTRIBUTE);

        if (certificates == null || certificates.length == 0) {
            return null;
        }

        // 只取终端客户端证书
        return certificates[0];
    }

    // 从 Nginx 转发的请求头中解析客户端证书 Nginx 使用 $ssl_client_escaped_cert 转发 URL 编码后的 PEM 证书
    private static X509Certificate resolveForwardedCertificate(ServerRequest request, String headerName) {
        String encodedCertificate = request.headers().firstHeader(headerName);

        if (encodedCertificate.isBlank()) {
            return null;
        }

        try {
            // URL 解码，恢复 PEM 证书内容
            String pemCertificate = UriUtils.decode(encodedCertificate, StandardCharsets.UTF_8);

            // PEM -> X509Certificate
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pemCertificate.getBytes(StandardCharsets.US_ASCII))) {
                return (X509Certificate) certificateFactory.generateCertificate(inputStream);
            }

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid forwarded client certificate", e);
        }
    }

}
