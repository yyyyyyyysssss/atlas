package com.atlas.notification.domain.entity;

import com.atlas.common.core.api.notification.enums.RenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationContent implements Serializable {

    private RenderType renderType;

    private Object body;

    private Map<String, Object> extra;

}
