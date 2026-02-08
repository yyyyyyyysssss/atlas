package com.atlas.notification.domain.mode;

import com.atlas.notification.enums.DisplayType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 9:33
 */
@Getter
@Setter
public abstract class MessagePayload {

    protected String title;

    // 外部服务商模板编码
    protected String extTemplateCode;

    // 渲染时使用的原始占位符变量
    protected Map<String, Object> params = new HashMap<>();

    // 扩展字段
    private Map<String, Object> ext = new HashMap<>();

    public void validate() {
        if (StringUtils.isNotEmpty(title) && title.length() > 100) {
            throw new IllegalArgumentException("消息标题过长");
        }
        doValidate();
    }

    protected abstract DisplayType getDisplayType();

    protected abstract void doValidate();

}
