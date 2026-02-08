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

    private UserContext userContext;

    public BaseMetaHandler(UserContext userContext){
        this.userContext = userContext;
    }

    @Override
    public void insertFill(MetaObject metaObject) {

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        this.strictInsertFill(metaObject, "creatorId", Long.class, userContext.getUserId());
        this.strictInsertFill(metaObject, "creatorName", String.class, userContext.getFullName());

        this.strictInsertFill(metaObject, "updaterId", Long.class, userContext.getUserId());
        this.strictInsertFill(metaObject, "updaterName", String.class, userContext.getFullName());

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);

        this.setFieldValByName("updaterId", userContext.getUserId(), metaObject);
        this.setFieldValByName("updaterName", userContext.getFullName(), metaObject);
    }


}
