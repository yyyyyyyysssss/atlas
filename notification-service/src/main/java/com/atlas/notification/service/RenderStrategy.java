package com.atlas.notification.service;


import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;
import com.atlas.notification.enums.DisplayType;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 9:39
 */
public interface RenderStrategy {

    boolean support(DisplayType displayType);

    MessagePayload render(MessageTemplateModel template, Map<String, Object> params, Map<String, Object> ext);

}
