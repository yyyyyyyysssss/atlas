package com.atlas.common.core.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    // 映射到表中的组织ID字段名
    String orgField() default "org_id";

    // 映射到表中的用户ID字段名
    String userField() default "creator_id";

    //指定需要过滤的别名
    String alias() default "";

    //指定需要过滤的表名
    String tableName() default "";

}
