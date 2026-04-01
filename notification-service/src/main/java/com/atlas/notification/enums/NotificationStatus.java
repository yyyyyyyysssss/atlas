package com.atlas.notification.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/1 10:04
 */
@Getter
@AllArgsConstructor
public enum NotificationStatus implements MybatisBaseEnum<String> {

    SENDING("发送中"),

    SENT("已发出"),

    FAILED("发送失败"),
            ;

    private final String description;

    @Override
    public String getCode() {
        return this.name();
    }

}
