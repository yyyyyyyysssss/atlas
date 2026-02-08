package com.atlas.notification.config.idwork;


import com.atlas.common.core.idwork.RandomWorkIdService;
import com.atlas.common.core.idwork.SnowflakeIdWorker;
import com.atlas.common.core.idwork.WorkIdService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description
 * @Author ys
 * @Date 2024/4/28 11:10
 */
@Configuration
public class IdConfig {


    // 测试使用
    @Bean
    public WorkIdService workIdService(){

        return new RandomWorkIdService();
    }

    @Bean
    public SnowflakeIdWorker snowflakeIdWorker(WorkIdService workIdService){

        return new SnowflakeIdWorker(workIdService);
    }

}
