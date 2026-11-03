package com.atlas.auth.domain.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record UserInfo(

        // 用户唯一标识
        String sub,

        // 用户名
        String preferredUsername,

        // 姓名
        String name,

        // 头像
        String picture,

        // 邮箱
        String email,

        // 邮箱是否已验证
        Boolean emailVerified,

        // 手机号
        String phoneNumber,

        // 手机号是否已验证
        Boolean phoneNumberVerified,

        // 扩展属性
        Map<String, Object> attributes

) {


    /**
     * 转换为统一用户属性 Map
     */
    public Map<String, Object> attributes() {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("sub", sub);
        result.put("preferred_username", preferredUsername);
        result.put("name", name);
        result.put("picture", picture);
        result.put("email", email);
        result.put("email_verified", emailVerified);
        result.put("phone_number", phoneNumber);
        result.put("phone_number_verified", phoneNumberVerified);

        if (attributes != null) {
            attributes.forEach(result::putIfAbsent);
        }

        return result;
    }

}
