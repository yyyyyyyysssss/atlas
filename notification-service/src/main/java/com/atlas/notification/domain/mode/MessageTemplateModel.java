package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 16:08
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageTemplateModel {

    private Long templateId;

    private String templateCode;

    private String title;

    private Object content;

    private ContentType contentType;

    // 外部对接
    private String extTemplateCode; // 第三方服务商 ID

}
