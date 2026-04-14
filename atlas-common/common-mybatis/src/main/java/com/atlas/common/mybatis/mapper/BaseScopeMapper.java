package com.atlas.common.mybatis.mapper;

import com.atlas.common.core.annotation.DataPermission;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BaseScopeMapper<T> extends BaseMapper<T> {

    @DataPermission
    default List<T> selectListScope(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        return this.selectList(queryWrapper);
    }

    @DataPermission
    default T selectOneScope(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        return this.selectOne(queryWrapper);
    }

}
