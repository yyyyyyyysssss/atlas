package com.atlas.common.mybatis.autoconfigure;

import com.atlas.common.core.autoconfigure.AtlasCoreAutoConfiguration;
import com.atlas.common.mybatis.injector.MySqlInjector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = AtlasCoreAutoConfiguration.class)
public class AtlasMybatisAutoConfiguration {

    @Bean
    public MySqlInjector mySqlInjector(){

        return new MySqlInjector();
    }


}
