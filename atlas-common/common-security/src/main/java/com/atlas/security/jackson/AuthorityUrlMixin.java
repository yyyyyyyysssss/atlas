package com.atlas.security.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 10:07
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonDeserialize(using = AuthorityUrlDeserializer.class)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AuthorityUrlMixin {
}
