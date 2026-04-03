package com.atlas.notification.service.render;

import com.atlas.notification.domain.mode.JsonPayload;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
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
public class JsonRenderStrategy extends AbstractRenderStrategy implements RenderStrategy {


    @Override
    public boolean support(ContentType contentType) {
        return contentType.equals(ContentType.JSON);
    }

    @Override
    protected MessagePayload doRender(MessageTemplateModel template, Map<String, Object> params) {
        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        StringSubstitutor sub = new StringSubstitutor(safeParams);
        // 支持默认值写法 ${var:default}
        sub.setEnableSubstitutionInVariables(true);
        String renderedTitle = sub.replace(template.getTitle());
        JsonPayload jsonPayload = new JsonPayload();
        jsonPayload.setTitle(renderedTitle);
        jsonPayload.setRenderType(template.getRenderType());
        if (template.getContent() instanceof String contentStr) {
            String renderedJson = sub.replace(contentStr);
            jsonPayload.setBody(renderedJson);
        } else {
            jsonPayload.setBody(template.getContent());
        }
        return jsonPayload;
    }

}
