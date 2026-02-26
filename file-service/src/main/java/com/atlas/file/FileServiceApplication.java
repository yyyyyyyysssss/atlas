package com.atlas.file;

import com.atlas.common.core.annotation.EnableAtlasFeign;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@EnableAsync
@EnableAtlasFeign
@SpringBootApplication
public class FileServiceApplication {

    public static void main(String[] args) {
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(timeZone);
        SpringApplication.run(FileServiceApplication.class, args);
    }

}
