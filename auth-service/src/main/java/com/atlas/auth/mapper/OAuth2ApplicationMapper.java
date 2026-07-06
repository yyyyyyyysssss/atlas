package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuth2ApplicationMapper extends BaseMapper<OAuth2ClientApplication> {
}
