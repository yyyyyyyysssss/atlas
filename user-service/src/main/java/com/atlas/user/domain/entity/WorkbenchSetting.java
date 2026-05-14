package com.atlas.user.domain.entity;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkbenchSetting {

    // 快捷方式 ID 列表
    @Size(max = 10, message = "快捷方式最多设置10个")
    private List<String> shortcuts = new ArrayList<>();

}
