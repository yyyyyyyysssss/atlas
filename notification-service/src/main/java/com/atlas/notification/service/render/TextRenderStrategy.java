package com.atlas.notification.service.render;

import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
import com.atlas.notification.domain.mode.TextPayload;
import com.atlas.common.core.api.notification.enums.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/1 15:31
 */
@Component
@Slf4j
public class TextRenderStrategy extends AbstractRenderStrategy implements RenderStrategy {


    @Override
    public boolean support(ContentType contentType) {
        return contentType.equals(ContentType.TEXT);
    }

    @Override
    protected MessagePayload doRender(MessageTemplateModel template, Map<String, Object> params) {
        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        StringSubstitutor sub = new StringSubstitutor(safeParams);
        // 支持默认值写法 ${var:default}
        sub.setEnableSubstitutionInVariables(true);
        String renderedTitle = sub.replace(template.getTitle());
        TextPayload textPayload = new TextPayload();
        textPayload.setTitle(renderedTitle);
        // 将原材料 content 渲染为最终成品 result
        textPayload.setText(sub.replace(template.getContent()));
        return textPayload;
    }

}
