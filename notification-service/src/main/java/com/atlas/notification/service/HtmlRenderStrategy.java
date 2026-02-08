package com.atlas.notification.service;

import com.atlas.notification.domain.mode.HtmlPayload;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
import com.atlas.notification.enums.DisplayType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Collections;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 11:31
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HtmlRenderStrategy extends AbstractRenderStrategy implements RenderStrategy{

    private final SpringTemplateEngine springTemplateEngine;

    private static final String CLASSPATH_PREFIX = "classpath:";

    @Override
    public boolean support(DisplayType displayType) {
        return displayType == DisplayType.HTML;
    }

    @Override
    public MessagePayload doRender(MessageTemplateModel template, Map<String, Object> params) {
        if (template == null) {
            throw new IllegalArgumentException("Template model cannot be null");
        }
        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        // 准备 Thymeleaf 上下文
        Context context = new Context();
        context.setVariables(safeParams);

        // 渲染标题 标题依然使用简单的占位符替换
        StringSubstitutor sub = new StringSubstitutor(safeParams);
        // 开启此项可以支持如 ${var1_${var2}} 这种复杂的嵌套替换
        sub.setEnableSubstitutionInVariables(true);
        String renderedTitle = sub.replace(template.getTitle());

        // 渲染 HTML 内容
        String content = template.getContent();
        String renderedHtml;
        try {
            if(StringUtils.startsWith(content, CLASSPATH_PREFIX)){
                // ClassPath 文件模式
                // 去掉 "classpath:" 前缀，并去掉 ".html" 后缀（Thymeleaf 解析器会自动加）
                String templatePath = content.substring(CLASSPATH_PREFIX.length())
                        .replace(".html", "");
                renderedHtml = springTemplateEngine.process(templatePath, context);
            } else {
                // DB 字符串模式
                renderedHtml = springTemplateEngine.process(content, context);
            }
        }catch (Exception e){
            log.error("Html Template Render Failed, Error: {}", e.getMessage());
            renderedHtml = content;
        }
        HtmlPayload payload = new HtmlPayload();
        payload.setTitle(renderedTitle);
        payload.setHtml(renderedHtml);
        return payload;
    }
}
