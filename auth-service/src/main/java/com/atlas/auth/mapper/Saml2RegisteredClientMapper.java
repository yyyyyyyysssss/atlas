package com.atlas.auth.mapper;


import com.atlas.auth.domain.entity.Saml2RegisteredClient;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface Saml2RegisteredClientMapper extends BaseMapper<Saml2RegisteredClient> {
}
