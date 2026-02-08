package com.atlas.common.mybatis.autoconfigure;

import com.atlas.common.core.json.JacksonConfig;
import com.atlas.common.mybatis.injector.MySqlInjector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration(after = {JacksonConfig.class})
@Configuration(proxyBeanMethods = false)
public class AtlasMybatisAutoConfiguration {

    @Bean
    public MySqlInjector mySqlInjector(){

        return new MySqlInjector();
    }


}
