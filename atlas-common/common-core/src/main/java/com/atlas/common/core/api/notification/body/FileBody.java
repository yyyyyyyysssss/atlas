package com.atlas.common.core.api.notification.body;

import com.atlas.common.core.api.notification.enums.RenderType;
import lombok.Builder;
import lombok.Getter;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/2 10:24
 */
@Getter
@Builder
public class FileBody implements NotificationBody {

    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;

    @Override
    public RenderType getRenderType() { return RenderType.FILE; }

}
