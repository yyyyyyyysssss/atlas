package com.atlas.notification.config.thymeleaf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 11:50
 */
@Configuration
public class ThymeleafConfig {

    @Value("${spring.thymeleaf.cache:true}")
    private boolean isCache;

    @Bean
    public SpringTemplateEngine springTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        // 解析器 1: 负责处理 classpath: 路径的文件
        ClassLoaderTemplateResolver fileResolver = new ClassLoaderTemplateResolver();
        fileResolver.setPrefix("templates/"); // 放在 resources/templates 下
        fileResolver.setSuffix(".html");
        fileResolver.setCacheable(isCache);
        fileResolver.setTemplateMode(TemplateMode.HTML);
        fileResolver.setCharacterEncoding("UTF-8");
        fileResolver.setOrder(1);
        fileResolver.setCheckExistence(true); // 如果文件不存在，交给下一个解析器

        // 解析器 2: 负责直接处理数据库传来的 HTML 字符串
        StringTemplateResolver stringResolver = new StringTemplateResolver();
        fileResolver.setCacheable(isCache);
        stringResolver.setTemplateMode(TemplateMode.HTML);
        stringResolver.setOrder(2); // 优先级排在文件之后

        engine.addTemplateResolver(fileResolver);
        engine.addTemplateResolver(stringResolver);
        return engine;
    }

}
