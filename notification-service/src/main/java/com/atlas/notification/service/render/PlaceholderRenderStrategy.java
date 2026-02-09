package com.atlas.notification.service.render;

import com.atlas.common.core.utils.JsonUtils;
import com.atlas.notification.domain.mode.*;
import com.atlas.notification.enums.DisplayType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 9:44
 */
@Component
@Slf4j
public class PlaceholderRenderStrategy extends AbstractRenderStrategy implements RenderStrategy {

    @Override
    public boolean support(DisplayType displayType) {

        return displayType == DisplayType.TEXT
                || displayType == DisplayType.CARD
                || displayType == DisplayType.MEDIA;
    }

    @Override
    public MessagePayload doRender(MessageTemplateModel template, Map<String, Object> params) {
        if (template == null) {
            throw new IllegalArgumentException("Template model cannot be null");
        }
        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        StringSubstitutor sub = new StringSubstitutor(safeParams);
        // 支持默认值写法 ${var:default}
        sub.setEnableSubstitutionInVariables(true);
        String renderedTitle = sub.replace(template.getTitle());
        switch (template.getDisplayType()) {
            case TEXT:
                TextPayload textPayload = new TextPayload();
                textPayload.setTitle(renderedTitle);
                // 将原材料 content 渲染为最终成品 result
                textPayload.setText(sub.replace(template.getContent()));
                return textPayload;
            case CARD:
                try {
                    // JSON字符串
                    String rawJson = template.getContent();
                    // 执行占位符替换 (将 JSON 里的 ${var} 替换掉)
                    String renderedJson = sub.replace(rawJson);
                    CardPayload cardPayload = JsonUtils.parseObject(renderedJson, CardPayload.class);
                    cardPayload.setTitle(renderedTitle);
                    return cardPayload;
                }catch (Exception e){
                    log.error("Card Template Render Failed! Error: {}", e.getMessage());
                    CardPayload fallback = new CardPayload();
                    fallback.setTitle(renderedTitle);
                    fallback.setBody(sub.replace(template.getContent())); // 哪怕是错的 JSON，也作为文字发出去
                    return fallback;
                }
            case MEDIA:
                MediaPayload mediaPayload = new MediaPayload();
                mediaPayload.setTitle(renderedTitle);
                // 将原材料 content 渲染为最终成品 result
                mediaPayload.setUrl(sub.replace(template.getContent()));
                Object fileName = safeParams.getOrDefault("fileName", "file");
                mediaPayload.setFileName(sub.replace(String.valueOf(fileName)));
                return mediaPayload;
            default:
                log.error("Unsupported display type: {}", template.getDisplayType());
                return null;
        }
    }
}
