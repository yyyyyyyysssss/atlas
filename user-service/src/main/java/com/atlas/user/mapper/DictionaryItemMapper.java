package com.atlas.user.mapper;

import com.atlas.common.mybatis.mapper.TreeMapper;
import com.atlas.user.domain.entity.DictionaryItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictionaryItemMapper extends BaseMapper<DictionaryItem>, TreeMapper<DictionaryItem> {
}
