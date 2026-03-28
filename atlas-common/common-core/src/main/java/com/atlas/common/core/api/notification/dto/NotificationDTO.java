package com.atlas.common.core.api.notification.dto;

import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.enums.DisplayType;
import com.atlas.common.core.api.notification.enums.TargetType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/6 15:42
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    // 模板编码
    private String templateCode;

    // 可选标题
    private String title;

    private Object content;

    private DisplayType displayType;

    // 接收目标
    @NotEmpty(message = "接收人列表不能为空")
    private List<String> targets;

    @NotNull(message = "目标类型不能为空")
    private TargetType targetType;

    // 发送渠道
    @NotEmpty(message = "发送渠道不能为空")
    private List<ChannelType> channels;

    // 占位符变量
    private Map<String, Object> params;

    // 扩展参数
    private Map<String, Object> ext;

}
