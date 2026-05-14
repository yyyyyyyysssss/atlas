package com.atlas.user.domain.entity;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.Map;

@Data
public class UserSetting {

    @Valid
    private WorkbenchSetting workbench;

    private AppearanceSetting appearance;

    private Map<String, Boolean> notification;

}
