package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.UserWebauthnCredentials;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserWebauthnCredentialsMapper extends BaseMapper<UserWebauthnCredentials> {
}
