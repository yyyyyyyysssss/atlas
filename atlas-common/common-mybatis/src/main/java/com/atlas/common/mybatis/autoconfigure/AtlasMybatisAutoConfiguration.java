package com.atlas.common.mybatis.autoconfigure;

import com.atlas.common.core.autoconfigure.AtlasCoreAutoConfiguration;
import com.atlas.common.mybatis.handler.BaseMetaHandler;
import com.atlas.common.mybatis.handler.DataScopeHandler;
import com.atlas.common.mybatis.injector.MySqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = AtlasCoreAutoConfiguration.class)
public class AtlasMybatisAutoConfiguration {

    @Bean
    public MySqlInjector mySqlInjector(){

        return new MySqlInjector();
    }

    @Bean
    public BaseMetaHandler baseMetaHandler(){

        return new BaseMetaHandler();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 数据权限处理器
        DataScopeHandler dataScopeHandler = new DataScopeHandler();
        // 数据权限拦截器
        DataPermissionInterceptor dataPermissionInterceptor = new DataPermissionInterceptor(dataScopeHandler);
        // 添加到 MP 拦截器链中
        interceptor.addInnerInterceptor(dataPermissionInterceptor);
        return interceptor;
    }


}
