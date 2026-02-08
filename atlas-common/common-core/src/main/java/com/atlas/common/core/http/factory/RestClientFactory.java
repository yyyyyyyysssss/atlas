package com.atlas.common.core.http.factory;


import com.atlas.common.core.http.interceptor.RestClientLogInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
@Slf4j
public class RestClientFactory {

    private final ObjectMapper objectMapper;

    public RestClientFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RestClient create(ClientHttpRequestFactory requestFactory, Consumer<RestClient.Builder> customizer) {
        List<HttpMessageConverter<?>> messageConverters = defaultMessageConverters();

        return RestClient.builder()
                .requestFactory(requestFactory)
                .apply(customizer)
                .defaultStatusHandler(HttpStatusCode::isError, this::logError)
                .messageConverters(messageConverters)
                .requestInterceptor(new RestClientLogInterceptor())
                .build();
    }

    public RestClient create(ClientHttpRequestFactory requestFactory, String baseUrl, Consumer<RestClient.Builder> customizer) {
        List<HttpMessageConverter<?>> messageConverters = defaultMessageConverters();

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .apply(customizer)
                .defaultStatusHandler(HttpStatusCode::isError, this::logError)
                .messageConverters(messageConverters)
                .requestInterceptor(new RestClientLogInterceptor())
                .build();
    }

    private List<HttpMessageConverter<?>> defaultMessageConverters() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new ResourceHttpMessageConverter());
        converters.add(new FormHttpMessageConverter());
        converters.add(new MappingJackson2XmlHttpMessageConverter());
        return converters;
    }

    private void logError(HttpRequest req, ClientHttpResponse res) throws IOException {
        log.error("request error {} {}", req.getURI(), res.getStatusCode().value());
    }

}
