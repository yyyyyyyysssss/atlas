package com.atlas.auth;

import com.atlas.common.core.annotation.EnableAtlasFeign;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@EnableAsync
@EnableCaching
@EnableAtlasFeign
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(timeZone);
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
