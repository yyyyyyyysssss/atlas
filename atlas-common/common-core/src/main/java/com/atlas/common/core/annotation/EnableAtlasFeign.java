package com.atlas.common.core.annotation;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableFeignClients(basePackages = "com.atlas.common.core.api")
@ComponentScan(basePackages = "com.atlas.common.core.api")
public @interface EnableAtlasFeign {
}
