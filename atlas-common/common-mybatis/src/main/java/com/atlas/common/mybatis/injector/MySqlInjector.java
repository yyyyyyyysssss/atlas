package com.atlas.common.mybatis.injector;

import com.atlas.common.mybatis.mapper.TreeMapper;
import com.atlas.common.mybatis.methods.*;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.session.Configuration;

import java.util.List;


public class MySqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(new Configuration(),mapperClass, tableInfo);
        if (TreeMapper.class.isAssignableFrom(mapperClass)){
            methodList.add(new SelectChildrenById());
            methodList.add(new SelectChildrenByIds());
            methodList.add(new SelectParentById());
            methodList.add(new SelectParentByIds());
            methodList.add(new SelectLineageById());
        }
        return methodList;
    }
}
