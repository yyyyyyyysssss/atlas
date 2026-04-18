package com.atlas.notification.service.render;

import com.atlas.common.core.api.notification.enums.RenderType;
import com.atlas.notification.domain.mode.StructuredPayload;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
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
public class StructuredRenderStrategy extends AbstractRenderStrategy implements RenderStrategy {


    @Override
    public boolean support(RenderType renderType) {
        return renderType != RenderType.TEXT && renderType != RenderType.HTML;
    }

    @Override
    protected MessagePayload doRender(MessageTemplateModel template, Map<String, Object> params) {
        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        StringSubstitutor sub = new StringSubstitutor(safeParams);
        // 支持默认值写法 ${var:default}
        sub.setEnableSubstitutionInVariables(true);
        String renderedTitle = sub.replace(template.getTitle());
        StructuredPayload structuredPayload = new StructuredPayload();
        structuredPayload.setTitle(renderedTitle);
        structuredPayload.setRenderType(template.getRenderType());
        if (template.getContent() instanceof String contentStr) {
            String renderedJson = sub.replace(contentStr);
            structuredPayload.setBody(renderedJson);
        } else {
            structuredPayload.setBody(template.getContent());
        }
        return structuredPayload;
    }

}
