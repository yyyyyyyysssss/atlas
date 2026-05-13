package com.atlas.user.domain.entity;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationSetting {

    private Map<String, Boolean> preferences;

}
