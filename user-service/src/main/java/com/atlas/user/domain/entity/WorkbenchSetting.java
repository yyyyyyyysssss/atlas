package com.atlas.user.domain.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkbenchSetting {

    // 快捷方式 ID 列表
    private List<String> shortcuts = new ArrayList<>();

}
