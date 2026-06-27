package com.atlas.auth.domain.dto;

public interface BaseUrlAppliable<T> {

    /**
     * 将传入的 baseUrl 应用到配置对象中，并返回处理后的新实例
     * * @param baseUrl 基础网关或服务URL
     * @return 处理后的配置对象
     */
    T applyBaseUrl(String baseUrl);

}
