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

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        this.strictInsertFill(metaObject, "creatorId", Long.class, UserContext.getUserId());
        this.strictInsertFill(metaObject, "creatorName", String.class, UserContext.getFullName());

        this.strictInsertFill(metaObject, "updaterId", Long.class, UserContext.getUserId());
        this.strictInsertFill(metaObject, "updaterName", String.class, UserContext.getFullName());

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);

        this.setFieldValByName("updaterId", UserContext.getUserId(), metaObject);
        this.setFieldValByName("updaterName", UserContext.getFullName(), metaObject);
    }


}
