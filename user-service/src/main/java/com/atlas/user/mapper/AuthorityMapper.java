package com.atlas.user.mapper;

import com.atlas.common.mybatis.mapper.TreeMapper;
import com.atlas.user.domain.entity.Authority;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthorityMapper extends BaseMapper<Authority>, TreeMapper<Authority> {

}
