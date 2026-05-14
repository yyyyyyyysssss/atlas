package com.atlas.user.domain.entity;

import lombok.Data;

import java.util.Map;

@Data
public class UserSetting {

    private WorkbenchSetting workbench;

    private AppearanceSetting appearance;

    private Map<String, Boolean> notification;

}
