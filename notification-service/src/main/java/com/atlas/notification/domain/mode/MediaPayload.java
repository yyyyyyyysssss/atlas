package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.DisplayType;
import com.atlas.common.core.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 11:05
 */
@Getter
@Setter
public class MediaPayload extends MessagePayload{

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    @Override
    protected DisplayType getDisplayType() {
        return DisplayType.MEDIA;
    }

    @Override
    protected void doValidate() {

    }

    @Override
    public String getContent() {
        // 只返回 HTML 映射，保持数据库 content 极简
        Map<String, String> data = new HashMap<>();
        data.put("fileUrl", this.fileUrl);
        data.put("fileName", this.fileName);
        data.put("fileSize", this.fileSize != null ? fileSize.toString() : null);
        return JsonUtils.toJson(data);
    }
}
