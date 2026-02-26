package com.atlas.common.core.context;

import com.atlas.common.core.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserContext {

    private static final ThreadLocal<UserObject> USER_HOLDER = new ThreadLocal<>();

    public static void setUser(Long userId, String fullName) {
        USER_HOLDER.set(new UserObject(userId, fullName,true));
    }

    public static void setUser(Long userId, String fullName, boolean masking) {
        USER_HOLDER.set(new UserObject(userId, fullName,masking));
    }

    public static Long getRequiredUserId() {
        Long id = getUserId();
        if (id == null) {
            throw new BusinessException("用户未登录");
        }
        return id;
    }

    public static Long getUserId() {
        UserObject user = USER_HOLDER.get();
        return user != null ? user.getUserId() : null;
    }

    public static String getFullName(){
        UserObject user = USER_HOLDER.get();
        return user != null ? user.getFullName() : null;
    }

    public static boolean isMasking() {
        UserObject user = USER_HOLDER.get();
        // 如果没有用户信息，默认开启脱敏，保证安全隐患最小化
        return user == null || user.isMasking();
    }

    public static UserObject getUser() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserObject {
        private Long userId;
        private String fullName;
        private boolean masking;
    }

}
