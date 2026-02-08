package com.atlas.notification;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@EnableAsync
@SpringBootApplication
@EnableCaching
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

    @PostConstruct
    public void init(){
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(timeZone);
    }


}
