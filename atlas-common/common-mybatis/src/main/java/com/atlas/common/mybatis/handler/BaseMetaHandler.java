package com.atlas.common.mybatis.handler;

import com.atlas.common.core.context.UserContext;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/17 11:26
 */

public class BaseMetaHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {

        LocalDateTime now = LocalDateTime.now();
        Long userId = UserContext.getUserId();
        String fullName = UserContext.getFullName();

        fillFieldIfNull(metaObject, "createTime", now);
        fillFieldIfNull(metaObject, "updateTime", now);
        fillFieldIfNull(metaObject, "creatorId", userId);
        fillFieldIfNull(metaObject, "creatorName", fullName);
        fillFieldIfNull(metaObject, "updaterId", userId);
        fillFieldIfNull(metaObject, "updaterName", fullName);

    }

    @Override
    public void updateFill(MetaObject metaObject) {

        forceUpdateFieldIfPresent(metaObject, "updateTime", LocalDateTime.now());
        forceUpdateFieldIfPresent(metaObject, "updaterId", UserContext.getUserId());
        forceUpdateFieldIfPresent(metaObject, "updaterName", UserContext.getFullName());
    }

    private void fillFieldIfNull(MetaObject metaObject, String fieldName, Object value) {
        if (metaObject.hasSetter(fieldName)) {
            // 使用底层策略，默认行为是：实体类中该字段为 null 才会填充
            this.strictFillStrategy(metaObject, fieldName, () -> value);
        }
    }

    private void forceUpdateFieldIfPresent(MetaObject metaObject, String fieldName, Object value) {
        // 确保实体类里有这个字段，没字段直接忽略，不报错
        if (metaObject.hasSetter(fieldName)) {
            // 这样就能无视实体类里原有的旧值，强行覆盖
            metaObject.setValue(fieldName, value);
        }
    }

}
