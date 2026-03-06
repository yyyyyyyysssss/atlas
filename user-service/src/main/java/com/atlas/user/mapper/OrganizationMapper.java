package com.atlas.user.mapper;

import com.atlas.common.mybatis.mapper.TreeMapper;
import com.atlas.user.domain.entity.Organization;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (Organization)表数据库访问层
 *
 * @author ys
 * @since 2026-02-28 16:21:33
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<Organization>,TreeMapper<Organization> {

    List<OrgMemberVO> findMembers(@Param("orgPath") String orgPath,@Param("includeChild") boolean includeChild);

}

