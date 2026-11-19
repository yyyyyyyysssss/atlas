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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/18 16:36
 */
public final class ClientCertificateFilterFunction {

    private static final String CERTIFICATE_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    private static final String CLIENT_CERT_HEADER = "Client-Cert";

    private static final String CLIENT_CERT_CHAIN_HEADER = "Client-Cert-Chain";

    private ClientCertificateFilterFunction() {
    }

    public static HandlerFilterFunction<ServerResponse, ServerResponse> forwardClientCertificate(boolean required, CertificateMode mode) {
        return HandlerFilterFunction.ofRequestProcessor(request -> {

            // 根据配置的模式获取客户端证书
            ClientCertificateChain certificate = switch (mode) {
                case LOCAL -> resolveLocalCertificate(request);
                case FORWARDED -> resolveForwardedCertificate(request);
            };
            if (certificate == null && required) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Client certificate is required");
            }
            try {
                String encodedCertificate = null;
                String encodedChain = null;
                if (certificate != null) {
                    // 编码终端客户端证书
                    encodedCertificate = encodeCertificate(certificate.certificate());

                    // 编码中间 CA 证书链
                    encodedChain = encodeCertificateChain(certificate.chain());
                }
                String finalCertificate = encodedCertificate;
                String finalChain = encodedChain;
                return ServerRequest.from(request)
                        .headers(headers -> {
                            // 清理外部传入的证书请求头
                            headers.remove(CLIENT_CERT_HEADER);
                            headers.remove(CLIENT_CERT_CHAIN_HEADER);
                            // 设置 RFC 9440 标准证书请求头
                            if (finalCertificate != null) {
                                headers.add(CLIENT_CERT_HEADER, finalCertificate);
                            }
                            // 证书链可能为空，属于正常情况
                            if (finalChain != null) {
                                headers.add(CLIENT_CERT_CHAIN_HEADER, finalChain);
                            }
                        })
                        .build();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to encode client certificate", e);
            }
        });
    }

    // 从本地 HTTPS Servlet 请求属性中获取证书链。
    private static ClientCertificateChain resolveLocalCertificate(ServerRequest request) {
        X509Certificate[] certificates = (X509Certificate[]) request
                .servletRequest()
                .getAttribute(CERTIFICATE_ATTRIBUTE);

        if (certificates == null || certificates.length == 0) {
            return null;
        }
        // 第一个证书为终端客户端证书
        X509Certificate certificate = certificates[0];

        // Client-Cert-Chain 不重复包含终端证书
        List<X509Certificate> chain = new ArrayList<>();
        for (int i = 1; i < certificates.length; i++) {
            chain.add(certificates[i]);
        }

        return new ClientCertificateChain(certificate, List.copyOf(chain));
    }

    /**
     * 从 Nginx 转发的 PEM 证书请求头中解析证书。
     * 约定：
     * Client-Cert: URL 编码后的 PEM 终端证书
     * Client-Cert-Chain: URL 编码后的 PEM 证书链，多个证书使用逗号分隔
     */
    private static ClientCertificateChain resolveForwardedCertificate(ServerRequest request) {
        String encodedCertificate = request.headers().firstHeader(CLIENT_CERT_HEADER);

        if (encodedCertificate.isBlank()) {
            return null;
        }

        try {
            // 解析终端客户端证书
            X509Certificate certificate = decodePemCertificate(encodedCertificate);

            // 解析中间 CA 证书链
            List<X509Certificate> chain = new ArrayList<>();
            String encodedChain = request.headers().firstHeader(CLIENT_CERT_CHAIN_HEADER);
            if (!encodedChain.isBlank()) {
                chain.addAll(decodePemCertificates(encodedChain));
            }

            return new ClientCertificateChain(certificate, List.copyOf(chain));

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid forwarded client certificate", e);
        }
    }

    // URL 编码 PEM -> X509Certificate。
    private static X509Certificate decodePemCertificate(String encoded) throws Exception {
        List<X509Certificate> certificates = decodePemCertificates(encoded);

        if (certificates.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one certificate");
        }

        return certificates.getFirst();
    }

    private static List<X509Certificate> decodePemCertificates(String encoded) throws Exception {

        String pem = UriUtils.decode(encoded, StandardCharsets.UTF_8);

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII))) {
            return factory.generateCertificates(inputStream)
                    .stream()
                    .map(X509Certificate.class::cast)
                    .toList();
        }
    }

    private static String encodeCertificate(X509Certificate certificate) throws CertificateEncodingException {
        String encoded = Base64.getEncoder().encodeToString(certificate.getEncoded());
        return ":" + encoded + ":";
    }

    /**
     * 编码证书链。
     * 多个证书使用逗号分隔。
     */
    private static String encodeCertificateChain(List<X509Certificate> certificates) throws CertificateEncodingException {

        if (certificates == null || certificates.isEmpty()) {
            return null;
        }

        List<String> encodedCertificates = new ArrayList<>();

        for (X509Certificate certificate : certificates) {
            encodedCertificates.add(encodeCertificate(certificate));
        }

        return String.join(", ", encodedCertificates);
    }


    // 客户端证书及其证书链。
    private record ClientCertificateChain(X509Certificate certificate, List<X509Certificate> chain) {
    }

}
