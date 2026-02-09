package com.atlas.user.mapper;

import com.atlas.common.mybatis.mapper.TreeMapper;
import com.atlas.user.domain.entity.Authority;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AuthorityMapper extends BaseMapper<Authority>, TreeMapper<Authority> {

    List<Authority> findMenuByRoleIds(@Param("roleIds") Collection<Long> roleIds);

}
