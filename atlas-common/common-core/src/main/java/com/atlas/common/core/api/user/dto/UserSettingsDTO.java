package com.atlas.common.core.api.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/14 10:51
 */
@Getter
@Setter
public class UserSettingsDTO {

    private Map<String, Boolean> notification;


    public Boolean isNotificationEnabled(String categoryName) {
        if (notification == null) return null;

        return notification.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(categoryName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

}
