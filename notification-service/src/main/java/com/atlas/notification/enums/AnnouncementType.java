package com.atlas.notification.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AnnouncementType implements MybatisBaseEnum<String> {

    URGENT("紧急"),
    RELEASE("发版"),
    NOTICE("通知"),
    MAINTAIN("维护"),
    ;

    private final String description;

    @Override
    public String getCode() {
        return this.name();
    }
}
