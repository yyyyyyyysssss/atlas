package com.atlas.notification.service.render;


import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
import com.atlas.common.core.api.notification.enums.ContentType;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 9:39
 */
public interface RenderStrategy {

    boolean support(ContentType contentType);

    MessagePayload render(MessageTemplateModel template, Map<String, Object> params, Map<String, Object> ext);

}
