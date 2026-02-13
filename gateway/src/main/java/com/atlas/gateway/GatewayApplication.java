package com.atlas.gateway;

import com.atlas.common.core.annotation.EnableAtlasFeign;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
@EnableAtlasFeign
public class GatewayApplication {

    public static void main(String[] args) {
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(timeZone);
        SpringApplication.run(GatewayApplication.class, args);
    }

}
