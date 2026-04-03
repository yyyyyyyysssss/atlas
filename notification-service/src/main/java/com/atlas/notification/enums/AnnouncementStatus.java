package com.atlas.notification.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AnnouncementStatus implements MybatisBaseEnum<String> {

    DRAFT("草稿"),
    PUBLISHED("已发布"),
    RECALLED("撤回"),
    ;

    private final String description;

    @Override
    public String getCode() {
        return this.name();
    }
}
