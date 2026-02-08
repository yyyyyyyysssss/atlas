package com.atlas.common.core.http.support;

import com.atlas.common.core.http.factory.HttpClientFactory;
import com.atlas.common.core.http.factory.RestClientFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

public class HttpInterfaceUtils {

    public static <T> T createClient(Class<T> clientClass, String baseUrl, HttpClientFactory hcf, RestClientFactory rcf) {
        RestClient restClient = rcf.create(hcf.create(), baseUrl, b -> {});
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientClass);
    }

}
