package com.atlas.notification.service.render;


import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.MessageTemplateModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 17:09
 */
public abstract class AbstractRenderStrategy implements RenderStrategy{

    @Override
    public MessagePayload render(MessageTemplateModel template, Map<String, Object> params, Map<String, Object> ext) {
        // 执行具体的渲染逻辑（由子类实现）
        MessagePayload payload = doRender(template, params);

        // 填入外部编码
        payload.setExtTemplateCode(template.getExtTemplateCode());

        // 复制原始变量
        Map<String, Object> finalParams = new HashMap<>();
        if (params != null) {
            finalParams.putAll(params);
        }
        payload.setParams(finalParams);

        // 复制扩展字段
        Map<String, Object> finalExt = new HashMap<>();
        if (ext != null) {
            finalExt.putAll(ext);
        }
        payload.setExt(finalExt);

        // 强制自检
        payload.validate();
        return payload;
    }

    protected abstract MessagePayload doRender(MessageTemplateModel model, Map<String, Object> params);
}
