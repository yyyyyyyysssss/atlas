package com.atlas.common.mybatis.mapper;

public interface TreeRelation {

    default String parentFieldName(){
        return "parent_id";
    }

    default String childFieldName(){
        return "id";
    }

}
