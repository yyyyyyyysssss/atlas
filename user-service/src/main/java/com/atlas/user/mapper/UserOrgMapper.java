package com.atlas.user.mapper;

import com.atlas.user.domain.dto.UserOrgDTO;
import com.atlas.user.domain.entity.UserOrg;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (UserOrg)表数据库访问层
 *
 * @author ys
 * @since 2026-03-03 11:08:40
 */
@Mapper
public interface UserOrgMapper extends BaseMapper<UserOrg> {


    List<UserOrg> selectByPairs(@Param("list") List<UserOrgDTO> list);

    int batchDeletePairwise(@Param("list") List<UserOrgDTO> list);

    
}

