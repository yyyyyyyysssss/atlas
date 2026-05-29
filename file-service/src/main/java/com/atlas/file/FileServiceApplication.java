package com.atlas.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@EnableAsync
@SpringBootApplication
public class FileServiceApplication {

    public static void main(String[] args) {
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(timeZone);
        SpringApplication.run(FileServiceApplication.class, args);
    }

}
