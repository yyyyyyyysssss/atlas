package com.atlas.common.core.http.interceptor;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RestClientLogInterceptor implements ClientHttpRequestInterceptor {

    // 限制日志记录的最大长度16KB，超过部分截断
    private static final int MAX_LOG_BODY_SIZE = 1024 * 16;
    @Override
    public ClientHttpResponse intercept(HttpRequest request, @NotNull byte[] body, @NotNull ClientHttpRequestExecution execution) throws IOException {
        MediaType reqType = request.getHeaders().getContentType();
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append(String.format("Request: [%s] %s", request.getMethod(), request.getURI()));
        // 仅在 Content-Type 为 JSON 或 Text 类型时记录请求体内容
        if(isJsonOrText(reqType) && body.length > 0){
            String requestBody = body.length > MAX_LOG_BODY_SIZE
                    ? new String(body, 0, MAX_LOG_BODY_SIZE, StandardCharsets.UTF_8) + "... [truncated]"
                    : new String(body, StandardCharsets.UTF_8);
            logBuilder.append(String.format("   ,Args: %s", requestBody));
        } else {
            logBuilder.append("   ,Args: [Content omitted due to type]");
        }
        // 记录请求开始时间
        long s = System.currentTimeMillis();
        ClientHttpResponse response;
        try {
            // 执行请求
            response = execution.execute(request, body);
        } catch (Exception e) {
            long diff = System.currentTimeMillis() - s;
            logBuilder.append(String.format("   Spend: %.3fs", diff / 1000.0));
            logBuilder.append(String.format("   ,Exception: %s", e.getMessage()));
            log.error(logBuilder.toString());
            throw e;
        }
        // 记录响应的时间
        long diff = System.currentTimeMillis() - s;
        logBuilder.append(String.format("   Spend: %.3fs", diff / 1000.0));
        // 获取响应的 Content-Type
        MediaType resType = response.getHeaders().getContentType();
        // 仅在 Content-Type 为 JSON 或 Text 类型时记录响应体内容
        if(isJsonOrText(resType)){
            long contentLength = response.getHeaders().getContentLength();
            // 如果 Content-Length 明确超过阈值，直接跳过读取
            if(contentLength > MAX_LOG_BODY_SIZE * 5){
                logBuilder.append(String.format(" ,Return: [Content too large to log, Size: %s]", formatSize(contentLength)));
                log.info(logBuilder.toString());
                return response;
            }
            InputStream inputStream = response.getBody();
            byte[] responseBody = StreamUtils.copyToByteArray(inputStream);
            logBuilder.append(String.format("   ,Return: %s", new String(responseBody,StandardCharsets.UTF_8)));
            log.info(logBuilder.toString());
            // 创建新的响应对象并返回缓存的响应体
            return new ClientHttpResponseWrapper(response, responseBody);
        }
        // 对于非 JSON 或 Text 类型的响应，跳过响应体日志记录
        logBuilder.append("   ,Return: [Content omitted due to type]");
        log.info(logBuilder.toString());
        return response;
    }

    private boolean isJsonOrText(MediaType mediaType) {
        if (mediaType == null){
            return false;
        }
        return mediaType.includes(MediaType.APPLICATION_JSON) ||
                mediaType.includes(MediaType.TEXT_PLAIN) ||
                mediaType.includes(MediaType.APPLICATION_XML) ||
                mediaType.getType().equals("text");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.2fKB", bytes / 1024.0);
        return String.format("%.2fMB", bytes / (1024.0 * 1024.0));
    }

    private record ClientHttpResponseWrapper(ClientHttpResponse originalResponse,
                                             byte[] responseBody) implements ClientHttpResponse {
        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return originalResponse.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return originalResponse.getStatusText();
        }

        @Override
        public void close() {
            originalResponse.close();
        }

        @Override
        public InputStream getBody() {

            return new ByteArrayInputStream(responseBody);
        }

        @Override
        public HttpHeaders getHeaders() {
            return originalResponse.getHeaders();
        }
    }

}
