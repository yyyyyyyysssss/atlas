package com.atlas.common.core.api.user.dto;

import java.util.Map;

public record CreateUserSpec(
        String fullName,        // 用户显示名
        String avatarUrl,          // 可选头像
        Map<String,String> extra   // 扩展字段
) {

    public static CreateUserSpec empty() {
        return new CreateUserSpec(null, null, null);
    }

    public static CreateUserSpec of(String fullName, String avatarUrl, Map<String,String> extra) {
        return new CreateUserSpec(fullName, avatarUrl, extra);
    }

}
