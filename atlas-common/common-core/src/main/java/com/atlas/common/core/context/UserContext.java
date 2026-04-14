package com.atlas.common.core.context;

import com.atlas.common.core.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

public class UserContext {

    private static final ThreadLocal<UserObject> USER_HOLDER = new ThreadLocal<>();

    public static void setUser(String userId, String orgId, String fullName, Set<String> dataScopes, String masking) {
        USER_HOLDER.set(new UserObject(userId, orgId, fullName, dataScopes, masking));
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

    public static Long getOrgId() {
        UserObject user = USER_HOLDER.get();
        return user != null ? user.getOrgId() : null;
    }

    public static Long getRequiredOrgId() {
        Long orgId = getOrgId();
        if (orgId == null) {
            throw new BusinessException("用户未配置组织");
        }
        return orgId;
    }

    public static String getFullName() {
        UserObject user = USER_HOLDER.get();
        return user != null ? user.getFullName() : null;
    }

    public static Set<String> getDataScopes() {
        UserObject user = USER_HOLDER.get();
        return user != null ? user.getDataScopes() : null;
    }

    public static Set<String> getRequiredDataScope() {
        Set<String> dataScopes = getDataScopes();
        if (dataScopes == null) {
            throw new BusinessException("用户未配置权限");
        }
        return dataScopes;
    }

    public static boolean isMasking() {
        UserObject user = USER_HOLDER.get();
        // 如果没有用户信息，默认开启脱敏，保证安全隐患最小化
        return user == null || user.isMasking();
    }

    public static UserObject getUser() {
        return USER_HOLDER.get();
    }

    public static void setUser(UserObject userObject) {
        USER_HOLDER.set(userObject.clone());
    }

    public static void clear() {
        USER_HOLDER.remove();
    }


    @Data
    public static class UserObject implements Cloneable {
        public UserObject(String userId, String orgId, String fullName, Set<String> dataScopes, String masking){
            if(userId != null && !userId.isEmpty()){
                this.userId = Long.parseLong(userId);
            }
            if(orgId != null && !orgId.isEmpty()){
                this.orgId = Long.parseLong(orgId);
            }
            if(fullName != null && !fullName.isEmpty()){
                this.fullName = fullName;
            }
            if(dataScopes != null && !dataScopes.isEmpty()){
                this.dataScopes = dataScopes;
            }
            if(masking != null && !masking.isEmpty()){
                this.masking = Boolean.parseBoolean(masking);
            } else {
                this.masking = true;
            }
        }
        private Long userId;
        private Long orgId;
        private String fullName;
        private Set<String> dataScopes;
        private boolean masking;

        @Override
        public UserObject clone() {
            try {
                return (UserObject) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

}
